package qasino.simulation.qasino;

import peersim.core.Node;
import qasino.simulation.rps.IMessage;

import java.util.List;

/**
 * Message containing the sample to exchange in Snob
 */
public class QasinoMessage implements IMessage {

    private List<Node> sample;

    public QasinoMessage(List<Node> sample) {
        this.sample = sample;
    }

    public Object getPayload() {
        return this.sample;
    }
}
