package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import snob.simulation.rps.IMessage;

import java.util.Iterator;
import java.util.Map;

public class SnobTpqsResponse implements IMessage {
    private Map<Triple, Iterator<Triple>> tpatterns;

    public SnobTpqsResponse(Map<Triple, Iterator<Triple>> tpatterns) {
        this.tpatterns = tpatterns;
    }

    public Map<Triple, Iterator<Triple>> getPayload() {
        return this.tpatterns;
    }
}
