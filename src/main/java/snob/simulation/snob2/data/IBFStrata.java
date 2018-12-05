package snob.simulation.snob2.data;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;
import com.google.common.hash.Hashing;
import org.apache.jena.graph.Triple;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.snob2.data.Strata.StrataEstimator;

import java.util.*;
import java.util.stream.Collectors;

public class IBFStrata {
    public static final int ibfSize = 1000;
    public final int constant = 2; // 1.5 -> 2 (-_-)"
    public IBF ibf = new IBF(ibfSize); // 2*500 diff
    public int count = 0;
    public Map<Integer, Integer> visited = new LinkedHashMap<>();
    public Map<Integer, Triple> data = new LinkedHashMap<>();
    private StrataEstimator estimator = new StrataEstimator(32);

    public static IBFStrata createIBFFromTriples(List<Triple> list) {
        IBFStrata ib = new IBFStrata();
        ib.insert(list);
        return ib;
    }

    public StrataEstimator getEstimator() {
        return estimator;
    }

    public List<Triple> getTriples() {
        List<Triple> toreturn = new LinkedList<>();
        for (Map.Entry<Integer, Triple> entry : data.entrySet()) toreturn.add(entry.getValue());
        return toreturn;
    }

    public IBF[] insert(List<Triple> triples) {
        // System.err.println("Inserting data into the strata estimator...");
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
        List<Integer> tis = new ArrayList<>();
        while (triples.hasNext()) {
            Triple triple = triples.next();
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
        this.ibf.add(hashed);
    }

    public int hash(Triple triple) {
        return Hashing.murmur3_32().hashBytes(triple.toString().getBytes()).asInt(); // match the 32
    }
}
