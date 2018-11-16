package snob.simulation.snob2.data;

import org.apache.jena.graph.Triple;
import se.rosenbaum.iblt.Cell;
import se.rosenbaum.iblt.data.IntegerData;
import se.rosenbaum.iblt.IBLT;
import se.rosenbaum.iblt.hash.HashFunction;
import se.rosenbaum.iblt.hash.IntegerDataHashFunction;
import se.rosenbaum.iblt.hash.IntegerDataSubtablesHashFunctions;
import se.rosenbaum.iblt.util.ResidualData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvertibleBloomFilter {
    private IBLT<IntegerData, IntegerData> iblt;
    private Map<IntegerData, HashedTriple> mapping;
    private int cellCount;
    private int hashFunctionCount;

    public Map<IntegerData, HashedTriple> getMapping() {
        return mapping;
    }
    public IBLT<IntegerData, IntegerData> getIblt() {
        return iblt;
    }

    public InvertibleBloomFilter(int cellCount, int hashFunctionCount) {
        this.cellCount = cellCount;
        this.hashFunctionCount = hashFunctionCount;
        iblt = new IBLT<>(createIntegerCells(cellCount), new IntegerDataSubtablesHashFunctions(cellCount, hashFunctionCount));
        mapping = new HashMap<>();
    }

    public Map<IntegerData, IntegerData> mydata () {
        Map<IntegerData, IntegerData> res = new HashMap<>();
        mapping.forEach((k, v) -> {
            res.put(k, data(v.getValue().asInt()));
        });
        return res;
    }

    public Triple get (Triple t) throws Exception {
        if(mapping.containsKey(t)) {
            HashedTriple h = mapping.get(t);
            IntegerData res = this.iblt.get(data(h.getKey().asInt()));
            if(res == null) {
                return null;
            } else {
                // chech if checksum is the same
                if (data(h.getValue().asInt()) == res) {
                    return t;
                } else {
                    throw new Exception("checkSum different for the triple: " + t.toString());
                }
            }
        } else {
            throw new Exception("Not found: " + t.toString());
        }
    }

    public void insert(Triple t) {
        IntegerData key = data(HashedTriple.hashKey(t).asInt());
        if (!mapping.containsKey(key)) {
            HashedTriple h = new HashedTriple(t);
            mapping.put(key, h);
            this.iblt.insert(key, data(h.getValue().asInt()));
        } else {
            return;
        }
    }

    /**
     * Return the list of triple that are not in the other Invertible Bloom Filter
     * @param b
     * @return List of triples
     */
    public List<Triple> absentTriple(InvertibleBloomFilter b) {
        return this._absentTriple(b.getIblt());
    }
    public List<Triple> _absentTriple(IBLT<IntegerData, IntegerData> incomingIBLT) {
        ResidualData<IntegerData, IntegerData> res = _reconcile(new IBLT<IntegerData, IntegerData>(incomingIBLT.getCells().clone(), new IntegerDataSubtablesHashFunctions(incomingIBLT.getCells().length, hashFunctionCount)), mydata());
        List<Triple> output = new ArrayList<>();
        res.getAbsentEntries().forEach((k, v) -> {
            System.out.printf("Absent (key= %d, value=%d) %n", k.getValue(), v.getValue());
            mapping.forEach((kk, vv) -> {
                System.out.printf("key=%d value=%s%n", kk.getValue(), vv.getTriple().toString());
            });
            System.out.printf("k (%d) is in mapping: %b%n", k.getValue(), this.mapping.containsKey(k));
            output.add(this.mapping.get(k).getTriple());
        });
        return output;
    }



    public ResidualData<IntegerData, IntegerData> _reconcile(IBLT<IntegerData, IntegerData> clone, Map<IntegerData, IntegerData> incomingData) {
        for (Map.Entry<IntegerData, IntegerData> entry : incomingData.entrySet()) {
            clone.delete(entry.getKey(), entry.getValue());
        }

        ResidualData<IntegerData, IntegerData> residualData = clone.listEntries();
        if (residualData == null) {
            // Reconciliation not possible, since we couldn't list entries.
            return null;
        }
        return residualData;
    }

    private Cell<IntegerData, IntegerData>[] createIntegerCells(int numberOfCells) {
        Cell[] cells = new Cell[numberOfCells];
        HashFunction<IntegerData, IntegerData> hashFunction = new IntegerDataHashFunction();
        for (int i = 0; i < numberOfCells; i++) {
            cells[i] = new Cell(data(0), data(0), data(0), hashFunction, 0);
        }
        return cells;
    }

    private IntegerData data(int value) {
        return new IntegerData(value);
    }
}
