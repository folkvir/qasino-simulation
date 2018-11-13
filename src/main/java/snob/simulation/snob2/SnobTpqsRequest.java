package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import snob.simulation.rps.IMessage;

import java.util.List;

public class SnobTpqsRequest implements IMessage {
    private List<Triple> patterns;

    public SnobTpqsRequest(List<Triple> tpqs) {
        this.patterns = tpqs;
    }

    public List<Triple> getPayload() {
        return this.patterns;
    }
}
