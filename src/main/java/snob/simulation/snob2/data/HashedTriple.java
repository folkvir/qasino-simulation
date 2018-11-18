package snob.simulation.snob2.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.apache.jena.graph.Triple;

public class HashedTriple {
    private Triple triple;
    private HashCode key;
    private HashCode value;

    public HashedTriple(Triple t) {
        triple = t;
        key = hashKey(t);
        value = hashValue(t);
    }

    public static HashCode hashKey(Triple t) {
        return Hashing.murmur3_128().hashBytes(t.toString().getBytes());
    }

    public static HashCode hashValue(Triple t) {
        return Hashing.crc32().hashBytes(t.toString().getBytes());
    }

    public Triple getTriple() {
        return triple;
    }

    public HashCode getKey() {
        return key;
    }

    public HashCode getValue() {
        return value;
    }


}
