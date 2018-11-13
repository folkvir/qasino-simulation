package snob.simulation.snob;

import org.apache.jena.graph.Triple;
import snob.simulation.rps.IMessage;

import java.util.List;

public class SnobTpqsMessage implements IMessage {
    private List<Triple> tpqs;

    public SnobTpqsMessage(List<Triple> tpqs) {
        this.tpqs = tpqs;
    }

    public List<Triple> getPayload() {
        return this.tpqs;
    }
}
