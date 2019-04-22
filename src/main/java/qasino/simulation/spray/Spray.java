package qasino.simulation.spray;

import peersim.core.CommonState;
import peersim.core.Node;
import qasino.simulation.cyclon.Cyclon;
import qasino.simulation.rps.ARandomPeerSamplingProtocol;
import qasino.simulation.rps.IMessage;
import qasino.simulation.rps.IRandomPeerSampling;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Spray protocol
 */
public class Spray extends ARandomPeerSamplingProtocol implements
        IRandomPeerSampling {

    public static boolean start = false;
    // #A no configuration needed, everything is adaptive
    // #B no values from the configuration file of peersim
    // #C local variables
    public SprayPartialView partialView;
    public Long oldest = null;
    public List<Long> previousSample = new ArrayList<>();
    public List<Node> previousSampleNode = new ArrayList<>();
    public List<Long> previousPartialview = new ArrayList<>();
    public List<Node> previousPartialviewNode = new ArrayList<>();
    // for the estimator
    public double estimator = 0;
    public double m_estimator = 0;

    /**
     * Constructor of the Spray instance
     *
     * @param prefix the peersim configuration
     */
    public Spray(String prefix) {
        super(prefix);
        this.partialView = new SprayPartialView();
    }

    public Spray() {
        this.partialView = new SprayPartialView();
    }

    @Override
    public boolean pFail(List<Node> path) {
        // the probability is constant since the number of hops to establish
        // a connection is constant
        double pf = 1 - Math.pow(1 - ARandomPeerSamplingProtocol.fail, 6);
        return CommonState.r.nextDouble() < pf;
    }

    public void periodicCall() {
        if (this.isUp && this.partialView.size() > 0) {
            // #1 choose the peer to exchange with
            this.partialView.incrementAge();
            Node q = this.partialView.getOldest();
            Spray qSpray = (Spray) q
                    .getProtocol(ARandomPeerSamplingProtocol.pid);
            oldest = q.getID();
            boolean isFailedConnection = this.pFail(null);
            if (qSpray.isUp() && !isFailedConnection) {

                // #A if the chosen peer is alive, exchange
                List<Node> sample = this.partialView.getSample(this.node, q,
                        true);

                this.previousPartialviewNode = new ArrayList<>(this.getPeers(Integer.MAX_VALUE));
                this.previousPartialview = previousPartialviewNode.parallelStream().map(node -> node.getID()).collect(Collectors.toList());
                this.previousSampleNode = new ArrayList<>(sample);
                this.previousSample = previousSampleNode.parallelStream().map(node -> node.getID()).collect(Collectors.toList());


                IMessage received = qSpray.onPeriodicCall(this.node,
                        new SprayMessage(sample));
                List<Node> samplePrime = (List<Node>) received.getPayload();
                this.partialView.mergeSample(this.node, q, samplePrime, sample,
                        true);

                if (start) {
                    this.processEstimatorWithPeer(qSpray);
                }
            } else {
                // #B run the appropriate procedure
                if (!qSpray.isUp()) {
                    this.onPeerDown(q);
                } else if (isFailedConnection) {
                    this.onArcDown(q);
                }
            }
        }
    }

    private void processEstimatorWithPeer(Spray node) {
        if (this.estimator == 0) {
            this.estimator = Math.exp(this.partialView.size());
        }
        if (node.estimator == 0) {
            node.estimator = Math.exp(node.partialView.size());
        }
        double est = (this.estimator + node.estimator) / 2;
        this.estimator = est;
        node.estimator = est;
        this.m_estimator++;
    }

    private void processEstimatorWithPV() {
        // then for each peer of the pv, process 2 by 2 the average both values
        for (Node peer : this.getAliveNeighbors()) {
            processEstimatorWithPeer((Spray) node.getProtocol(ARandomPeerSamplingProtocol.pid));
        }
    }


    public double estimateSize() {
        return this.estimator;
    }

    public IMessage onPeriodicCall(Node origin, IMessage message) {
        List<Node> samplePrime = this.partialView.getSample(this.node, origin,
                false);
        this.partialView.mergeSample(this.node, origin,
                (List<Node>) message.getPayload(), samplePrime, false);
        return new SprayMessage(samplePrime);
    }

    public void join(Node joiner, Node contact) {
        if (this.node == null) { // lazy loading of the node identity
            this.node = joiner;
        }
        if (contact != null) { // the very first join does not have any contact
            Spray contactSpray = (Spray) contact.getProtocol(Cyclon.pid);
            this.partialView.clear();
            this.partialView.addNeighbor(contact);
            contactSpray.onSubscription(this.node);
        }
        this.isUp = true;
    }

    public void onSubscription(Node origin) {
        List<Node> aliveNeighbors = this.getAliveNeighbors();
        for (Node neighbor : aliveNeighbors) {
            Spray neighborSpray = (Spray) neighbor.getProtocol(Spray.pid);
            neighborSpray.addNeighbor(origin);
        }
    }

    public void leave() {
        this.isUp = false;
        this.partialView.clear();
        // nothing else
    }

    public List<Node> getPeers(int k) {
        return this.partialView.getPeers(k);
    }

    @Override
    public IRandomPeerSampling clone() {
        try {
            Spray sprayClone = new Spray();
            sprayClone.partialView = (SprayPartialView) this.partialView
                    .clone();
            return sprayClone;
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addNeighbor(Node peer) {
        return this.partialView.addNeighbor(peer);
    }

    /**
     * Procedure that handles the peer failures when detected
     *
     * @param q the peer supposedly crashed
     */
    public void onPeerDown(Node q) {
        // #1 probability to NOT recreate the connection
        double pRemove = 1.0 / this.partialView.size();
        // #2 remove all occurrences of q in our partial view and count them
        int occ = this.partialView.removeAll(q);
        if (this.partialView.size() > 0) {
            // #3 probabilistically double known connections
            for (int i = 0; i < occ; ++i) {
                if (CommonState.r.nextDouble() > pRemove) {
                    Node toDouble = this.partialView.getPeers().get(
                            CommonState.r.nextInt(this.partialView.size()));
                    this.partialView.addNeighbor(toDouble);
                }
            }
        }
    }

    /**
     * Replace an failed arc by an existing one
     *
     * @param q the destination of the arc to replace
     */
    public void onArcDown(Node q) {
        // #1 remove the unestablished link
        this.partialView.removeNode(q);
        // #2 double a known connection at random
        if (this.partialView.size() > 0) {
            Node toDouble = this.partialView.getPeers().get(
                    CommonState.r.nextInt(this.partialView.size()));
            this.partialView.addNeighbor(toDouble);
        }
    }

}
