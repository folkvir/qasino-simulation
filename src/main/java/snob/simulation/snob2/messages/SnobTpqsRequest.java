package snob.simulation.snob2.messages;

import org.apache.jena.graph.Triple;
import snob.simulation.rps.IMessage;
import snob.simulation.snob2.data.InvertibleBloomFilter;

import java.util.Map;

public class SnobTpqsRequest implements IMessage {
    private Map<Triple, InvertibleBloomFilter> patterns;


    public SnobTpqsRequest(Map<Triple, InvertibleBloomFilter> tpqs) {
        this.patterns = tpqs;
    }

    public Map<Triple, InvertibleBloomFilter> getPayload() {
        return this.patterns;
    }
}
