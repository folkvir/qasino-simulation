package qasino.simulation.spray;

import peersim.core.Node;
import qasino.simulation.rps.IMessage;

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
