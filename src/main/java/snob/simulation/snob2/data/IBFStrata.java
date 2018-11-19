package snob.simulation.snob2.data;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;
import com.google.common.hash.Hashing;
import org.apache.jena.graph.Triple;
import snob.simulation.snob2.Profile;
import snob.simulation.snob2.data.Strata.Cell;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.snob2.data.Strata.StrataEstimator;


import java.util.*;
import java.util.stream.Collectors;

public class IBFStrata  {
    public IBF ibf100 = new IBF(100);
    public IBF ibf1k = new IBF(1000);
    public IBF ibf10k = new IBF(10000);
    public IBF ibf100k = new IBF(100000);
    int estimateErrored = 0; // number of times the estimation return an estimation larger than 100k

    private Map<Integer, Triple> data = new HashMap<>();


    public StrataEstimator getEstimator() {
        return estimator;
    }

    private StrataEstimator estimator = new StrataEstimator(128);

    public IBF[] insert(List<Triple> triples) {
        System.err.println("Inserting data into the strat estimator...");
        List<Integer> s = triples.stream().map(triple -> {
            System.err.println("Inserting into strata IBF -> " + triple.toString());
            int hashed = hash(triple);
            _insert(hashed);
            this.data.put(hashed, triple);
            return hashed;
        }).collect(Collectors.toList());

        return estimator.encode(Ints.toArray(s));
    }

    private void _insert(int hashed) {
        this.ibf100.add(hashed);
        this.ibf1k.add(hashed);
        this.ibf10k.add(hashed);
        this.ibf100k.add(hashed);
    }

    private int hash (Triple triple) {
        return Hashing.murmur3_128().hashBytes(triple.toString().getBytes()).asInt(); // match the 32
    }


    public List<Triple> exchange(Triple pattern, Profile remote) {
        System.err.println("Exchange strata estimator with the remote to calculate the size of the IBF.");
        // simulate the exchange
        IBFStrata remoteIbfstrata = remote.strata.get(pattern);
        StrataEstimator remoteStrata = remoteIbfstrata.estimator;
        System.err.println("Compute the set difference size estimation...");
        int diffSize = remoteStrata.decode(this.estimator);
        // create the IBF of size diffSize with triple from the remote peer
        // for the simulation we previously loaded ibfs of 100/1k/10k/100k cells
        // and we return the corresponding ibf with data already inserted.
        IBF result;
        IBF us;
        int constant = 2; // 1.5 -> 2 (-_-)"
        System.err.println("Difference size estimation is: " + (diffSize));
        if((2 * diffSize) < 100) {
            // put it in the ibf100
            result = remoteIbfstrata.ibf100;
            us =  this.ibf100;
        } else if ((2 * diffSize) < 1000) {
            // put it in the ibf1000
            result = remoteIbfstrata.ibf1k;
            us =  this.ibf1k;
        } else if ((2 * diffSize) < 10000) {
            // put it in the ibf10000
            result = remoteIbfstrata.ibf10k;
            us =  this.ibf10k;
        } else if ((2 * diffSize) < 100000) {
            // put it in the ibf100000
            result = remoteIbfstrata.ibf100k;
            us =  this.ibf100k;
        } else {
            // hum hum too large? directly send triples....
            // perhaps ibf1M but seems to big
            estimateErrored++;
            // directly return, one roundtrip
            // on remote
            return remoteIbfstrata.data.values().parallelStream().collect(Collectors.toList());
        }


        System.err.println();
        // second round trip, on us
        // substract us from result then decode us from the difference
        System.err.println("Computing the set difference...");
        Cell[] cells = us.subtract(result.getCells()).clone();
        List<Integer>[] difference = us.decode(cells);
        Iterator<Integer> additional = difference[0].iterator();
        Iterator<Integer> miss = difference[1].iterator();
        // all triples in additionnal need to be ask from the remote
        // so simulate the second round trip
        List<Triple> triples = new ArrayList<>();
//        additional.forEachRemaining(hash -> {
//            System.err.println("Additional triples: " + this.data.get(hash).toString());
//        });
        miss.forEachRemaining(hash -> {
            System.err.println("Missing triples: " + remoteIbfstrata.data.get(hash).toString());
            triples.add(remoteIbfstrata.data.get(hash));
        });
        return triples;
    }
}
