package snob.simulation.snob2.data;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;
import com.google.common.hash.Hashing;
import org.apache.jena.graph.Triple;
import snob.simulation.snob2.Profile;
import snob.simulation.snob2.Snob;
import snob.simulation.snob2.data.Strata.Cell;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.snob2.data.Strata.StrataEstimator;


import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class IBFStrata  {
    public int constant = 2; // 1.5 -> 2 (-_-)"
    public IBF ibf100 = new IBF(100); // 2*50 diff
    public IBF ibf1k = new IBF(1000); // 2*500 diff
    public int estimateErrored = 0; // number of times the estimation return an estimation larger than 100k
    public int count = 0;
    private Map<Integer, Triple> data = new HashMap<>();
    public Map<Integer, Integer> visited = new HashMap<>();

    public StrataEstimator getEstimator() {
        return estimator;
    }

    private StrataEstimator estimator = new StrataEstimator(128);

    public IBF[] insert(List<Triple> triples) {
        System.err.println("Inserting data into the strata estimator...");
        List<Integer> s = triples.stream().map(triple -> {
            // System.err.println("Inserting into strata IBF -> " + triple.toString());
            int hashed = hash(triple);
            _insert(hashed);
            this.data.put(hashed, triple);
            count++;
            return hashed;
        }).collect(Collectors.toList());

        return estimator.encode(Ints.toArray(s));
    }
    public IBF[] insert(Iterator<Triple> triples) {
        System.err.println("Inserting data into the strata estimator...");
        List<Integer> tis = new ArrayList<Integer>();
        while(triples.hasNext()) {
            Triple triple = triples.next();
            // System.err.println("Inserting into strata IBF -> " + triple.toString());
            int hashed = hash(triple);
            _insert(hashed);
            this.data.put(hashed, triple);
            tis.add(hashed);
            int[] r = new int[1];
            r[0] = hashed;
            count++;
            estimator.encode(r);
        }
        return estimator.ibfs;
    }

    private void _insert(int hashed) {
        this.ibf100.add(hashed);
        this.ibf1k.add(hashed);
//        this.ibf10k.add(hashed);
//        this.ibf100k.add(hashed);
    }

    public int hash (Triple triple) {
        return Hashing.murmur3_128().hashBytes(triple.toString().getBytes()).asInt(); // match the 32
    }

    /**
     * The exchange is as follow:
     * 1) A send its Estimator and its IBF for a given triple pattern to B
     * 2) If B has a common triple pattern, B compute the difference size using estimator and send to A an IBF based on
     * its values
     * 2.1) A receives the IBF and make the set difference, ask to B for missing triples with a list of hashes.
     * 2.2) B answers with a list of triples from the list of hashes.
     * 3) Or instead of 2), If B don't have a common triple pattern, compute the triple pattern and check for the size of
     * both sets. If the set of B is twice the size of the set of A, directly send triples, otherwise check for membership
     * and send only triples not in the set of A
     * Finally, only a maximum of 2 rounds (4*l, l is the latency) and a minimum of 2 rounds (2l) is necessary for exchanging data.
     * Basically, Estimator(A)[pattern] + IBF(A)[pattern] + count[pattern] is sent to B
     * @param pattern
     * @param snob
     * @return
     */
    public List<Triple> exchange(Triple pattern, Snob snob) {
        return _exchange(pattern, snob.profile, snob.id);
    }

    public List<Triple> _exchange(Triple pattern, Profile remote, int id) {
        // System.err.printf("[IBF]  Exchanging (tp, Estimator, IBF, count) with peer number %d...%n", id);
        // simulate the exchange
        IBFStrata remoteIbfstrata = remote.strata.get(pattern);

        if(remoteIbfstrata == null) {
            if(!visited.containsKey(id)){
                visited.put(id, id);
                estimateErrored++;
                // get all triples matching the triple pattern
                Iterator<Triple> it = remote.datastore.getTriplesMatchingTriplePattern(pattern);
                List<Triple> result = new ArrayList<>();
                while(it.hasNext()) {
                    // check if
                    result.add(it.next());
                }
                // if size == 0 return an emtpty list
                if(result.size() == 0) return Collections.emptyList();
                // this.count is send to B with the estimator and the IBF of B
                if(result.size() * 2 > this.count) {
                    // the remote peer have 2 times more result than us, it is most likely he has interesting triples.
                    // System.err.printf("[IBF-no] Returning  %d triples directly %n", result.size());
                    return result;
                } else {
                    // check if triples are in IBF (2 second round)
                    List<Triple> finalresult = new ArrayList<>();
                    result.forEach(triple -> {
                        if(count < 100) {
                            if(remote.strata.get(pattern) != null && !remote.strata.get(pattern).ibf100.contains(remote.strata.get(pattern).hash(triple))) {
                                finalresult.add(triple);
                            }
                        } else if (count < 1000) {
                            if(remote.strata.get(pattern) != null && !remote.strata.get(pattern).ibf1k.contains(remote.strata.get(pattern).hash(triple))) {
                                finalresult.add(triple);
                            }
                        }
                    });
                    // System.err.printf("[IBF-no] Returning  %d triples after checking in the set. %n", finalresult.size());
                    return finalresult;
                }
            } else {
                // System.err.println("[IBF-yes] Returning an empty list.");
                return Collections.emptyList();
            }
        } else {
            StrataEstimator remoteStrata = remoteIbfstrata.estimator;
            // System.err.println("[IBF-yes] Compute the set difference size estimation...");
            int diffSize = remoteStrata.decode(this.estimator);

            if(diffSize == 0) {
                // System.err.println("[IBF-yes] Returning an empty list.");
                return Collections.emptyList();
            } else {
                // create the IBF of size diffSize with triple from the remote peer
                // for the simulation we previously loaded ibfs of 100/1k/10k/100k cells
                // and we return the corresponding ibf with data already inserted.
                IBF result;
                IBF us;

                // System.err.println("[IBF-yes] Difference size estimation is: |A-B| + |B-A| = " + (diffSize));
                if((2 * diffSize) < 100) {
                    // put it in the ibf100
                    result = remoteIbfstrata.ibf100;
                    us =  this.ibf100;
                } else if ((2 * diffSize) < 1000) {
                    // put it in the ibf1000
                    result = remoteIbfstrata.ibf1k;
                    us =  this.ibf1k;
                }else {
                    // hum hum too large? directly send triples....
                    // perhaps ibf1M but seems to big
                    estimateErrored++;
                    // directly return, one roundtrip
                    // on remote
                    return remoteIbfstrata.data.values().parallelStream().collect(Collectors.toList());
                }
                // System.err.println("[IBF-yes] common triple pattern.");
                // second round trip, on us
                // substract us from result then decode us from the difference
                // System.err.println("[IBF-yes] Computing the set difference...");
                Cell[] cells = us.subtract(result.getCells()).clone();
                List<Integer>[] difference = us.decode(cells);
                Iterator<Integer> additional = difference[0].iterator();
                Iterator<Integer> miss = difference[1].iterator();
                // all triples in additionnal need to be ask from the remote
                // so simulate the second round trip
                List<Triple> triples = new ArrayList<>();
//        while(additional.hasNext()) {
//            int hash = additional.next();
//            System.err.println("Additional triples: " + remoteIbfstrata.data.get(hash).toString());
//            triples.add(remoteIbfstrata.data.get(hash));
//        }
                while(miss.hasNext()) {
                    int hash = miss.next();
                    // System.err.println("[IBF-yes] Missing triples: " + remoteIbfstrata.data.get(hash).toString());
                    triples.add(remoteIbfstrata.data.get(hash));
                }
                return triples;
            }
        }
    }
}
