package snob.simulation.spray;

import peersim.core.Node;
import snob.simulation.rps.IMessage;

import java.util.List;

public class SprayMessage implements IMessage {

    private List<Node> sample;

    public SprayMessage(List<Node> sample) {
        this.sample = sample;
    }

    public Object getPayload() {
        return this.sample;
    }
}
