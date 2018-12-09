package snob.simulation.snob2.data;

import com.github.jsonldjava.shaded.com.google.common.primitives.Ints;
import com.google.common.hash.Hashing;
import org.apache.jena.graph.Triple;
import snob.simulation.snob2.data.Strata.Cell;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.snob2.data.Strata.StrataEstimator;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class IBFStrata {
    public static final int ibfSize = 1000;
    public IBF ibf = new IBF(ibfSize); // 2*500 diff
    public int count = 0;
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
        List<Integer> res = new LinkedList<>();
        for (Triple triple : triples) {
            int hashed = triple.hashCode(); // hash(triple);
            if(!data.containsKey(hashed)) {
                // System.err.println("Insert triple in the ibf:" + triple);
                this.data.put(hashed, triple);
                this.ibf.add(hashed);
                count++;
                res.add(hashed);
            }
        }
        // System.err.println("Finally insert: " + res.size());
        return estimator.encode(Ints.toArray(res));
    }

    public class Result {
        public int diffsize = 0;
        public int messagessent = 0;
        public List<Triple> missing = new LinkedList<>();
    }
    public Result difference(IBFStrata remote) {
        Result res = new Result();
        res.messagessent++;
        // exchange estimator.
        int diffsize = remote.getEstimator().decode(this.getEstimator());
        // System.err.println("Diffsize: " + diffsize);
        res.diffsize = diffsize;
        if(diffsize == 0) {
            return res;
        } else {
            Cell[] cells = ibf.subtract(remote.ibf.getCells());
            List<Integer>[] difference = ibf.decode(cells);
            if(difference == null) {
                res.messagessent++;
                res.missing = remote.getTriples();
                return res;
            } else {
                // send missing hash to B
                res.messagessent++;
                // B send us missing triples
                res.missing = difference[1].parallelStream().map(hash -> remote.data.get(hash)).collect(Collectors.toList());
                return res;
            }
        }
    }
}
