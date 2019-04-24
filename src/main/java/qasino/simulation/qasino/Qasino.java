package qasino.simulation.qasino;

import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import qasino.simulation.qasino.data.IBFStrata;
import qasino.simulation.rps.ARandomPeerSamplingProtocol;
import qasino.simulation.rps.IMessage;
import qasino.simulation.rps.IRandomPeerSampling;
import qasino.simulation.spray.Spray;
import qasino.simulation.spray.SprayMessage;
import qasino.simulation.spray.SprayPartialView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Qasino extends Spray {
    private static final String PAR_TRAFFIC = "traffic"; // minimization of the traffic
    public static boolean traffic;
    public static boolean start = false;
    private static int snobs = 0;
    public final int id = Qasino.snobs++;
    public int shuffle = 0;
    // Profile of the peer
    public Profile profile = new Profile();
    public double messages = 0;
    public long tripleResponses = 0;
    public int observed = 0;

    // state based crdt counter Map(pattern, Map(id, counter))
    public StateBasedCrdtCounter crdt = new StateBasedCrdtCounter(this.id);


    public Long oldest = null;
    public List<Node> previousSampleNode = new ArrayList<>();
    public List<Node> previousPartialviewNode = new ArrayList<>();

    /**
     * Constructor of the Spray instance
     *
     * @param prefix the peersim configuration
     */
    public Qasino(String prefix) {
        super(prefix);
        Qasino.traffic = Configuration.getBoolean(prefix + "." + PAR_TRAFFIC);
        this.profile = new Profile();
        this.partialView = new SprayPartialView();
    }

    public Qasino() {
        this.partialView = new SprayPartialView();
    }

    public static Qasino fromNodeToSnobSpray(Node node) {
        return (Qasino) node.getProtocol(ARandomPeerSamplingProtocol.pid);
    }

    @Override
    public void periodicCall() {
        super.periodicCall();
        if (start) {
            shuffle++;
            // APPLY SNOB NOW
            List<Node> currentPartialview = this.getPeers(Integer.MAX_VALUE);
            List<Node> peers = computeRealNewPeers(this.node.getID(), this.oldest, previousPartialviewNode, previousSampleNode, currentPartialview);
            if (profile.has_query && !profile.query.terminated) {
                // 1 - send tpqs to neighbours and receive responses
                for (Node node_rps : peers) {
                    observed++;
                    this.exchangeTriplePatterns(fromNodeToSnobSpray(node_rps));
                    crdt.increment();
                    if (fromNodeToSnobSpray(node_rps).profile.has_query) {
                        fromNodeToSnobSpray(node_rps).exchangeTriplePatterns(this);
                        // warning only usable when the same query is executed in the network
                        // see message in crdt class for a real prototype
                        // update crdts on both side, synchronization
                        crdt.update(fromNodeToSnobSpray(node_rps).crdt);
                        fromNodeToSnobSpray(node_rps).crdt.update(this.crdt);
                    }
                }
                profile.execute();
            }
        }
    }

    @Override
    public void join(Node joiner, Node contact) {
        if (this.node == null) { // lazy loading of the node identity
            this.node = joiner;
        }
        if (contact != null) { // the very first join does not have any contact
            Qasino contactSpray = (Qasino) contact.getProtocol(Spray.pid);
            this.partialView.clear();
            this.partialView.addNeighbor(contact);
            contactSpray.onSubscription(this.node);
        }
        this.isUp = true;
    }

    @Override
    public void onSubscription(Node origin) {
        List<Node> aliveNeighbors = this.getAliveNeighbors();
        for (Node neighbor : aliveNeighbors) {
            Qasino neighborSpray = (Qasino) neighbor.getProtocol(Spray.pid);
            neighborSpray.addNeighbor(origin);
        }
    }

    @Override
    public IRandomPeerSampling clone() {
        try {
            Qasino sprayClone = new Qasino();
            sprayClone.partialView = (SprayPartialView) this.partialView.clone();
            return sprayClone;
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Exchange Triple patterns with Invertible bloom filters associated with.
     * In return the other peers send us missing triples for each triple pattern.
     * See onTpqs(...) function
     *
     * @param remote
     */
    protected void exchangeTriplePatterns(Qasino remote) {
        this.profile.query.patterns.forEach(pattern -> {
            if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
                if (traffic) {
                    exchangeTriplesFromPatternUsingIbf(remote, pattern);
                } else {
                    exchangeTriplesFromPatternWithoutIblt(remote, pattern);
                }
            } else {
                if (!this.profile.query.alreadySeen.containsKey(pattern) || !this.profile.query.alreadySeen.get(pattern).contains(remote.id)) {
                    exchangeTriplesFromPatternWithoutIblt(remote, pattern);
                }
            }
            this.profile.query.addAlreadySeen(pattern, remote.id, this.id);
            if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
                this.profile.query.mergeAlreadySeen(pattern, remote.profile.query.alreadySeen.get(pattern));
            }
        });
    }

    /**
     * Exchange triples without using iblt.
     *
     * @param remote
     * @param pattern
     */
    private void exchangeTriplesFromPatternWithoutIblt(Qasino remote, Triple pattern) {
        this.messages++;
        List<Triple> list;
        if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
            list = remote.profile.query.data.get(pattern).parallelStream().collect(Collectors.toList());
        } else {
            list = remote.profile.local_datastore.getTriplesMatchingTriplePatternAsList(pattern);
        }
        tripleResponses += list.size();
        this.profile.insertTriplesWithList(pattern, list);
    }

    /**
     * Exchange triple pattern using Invertible bloom filter, and share data if necessary
     *
     * @param remote
     * @param pattern
     */
    public void exchangeTriplesFromPatternUsingIbf(Qasino remote, Triple pattern) {
        // send the ibf to remote peer with the pattern
        if (!remote.profile.has_query || (remote.profile.has_query && !remote.profile.query.patterns.contains(pattern))) {
            // no synchronization as the other do not have any triple pattern in common. So send all triples

            // if remote does not have a query, get triple pattern, get ibf on this triple pattern, set reconciliation.
            List<Triple> computed = remote.profile.local_datastore.getTriplesMatchingTriplePatternAsList(pattern);
            this.messages++;
            if (computed.size() > 0) {
                this.profile.insertTriplesWithList(pattern, computed);
                this.tripleResponses += computed.size();
            }
        } else {
            // the remote peer has the pattern
            IBFStrata.Result res = this.profile.query.strata.get(pattern).difference(remote.profile.query.strata.get(pattern));
            if (res.messagessent == 0) {
                this.messages++;
            } else {
                this.messages += res.messagessent;
            }
            this.profile.insertTriplesWithList(pattern, res.missing);
            this.tripleResponses += res.missing.size();
        }
    }

    private List<Node> computeRealNewPeers(long id, Long oldest, List<Node> previousPartialview, List<Node> previousSample, List<Node> currentPartialview) {
        // firstly, remove the oldest from the old partialview
        List<Node> oldpv = new ArrayList<>(previousPartialview);
        oldpv.remove(oldest);
        // secondly, remove the id from the old sample
        List<Node> oldsample = new ArrayList<>(previousSample);
        oldsample.remove(id);
        // thridly, substract oldpv wiith oldsample
        oldpv.removeAll(oldsample);
        // then remove all entry in newpv containing remaining oldpv entries
        List<Node> remainings = new ArrayList<>(currentPartialview);
        remainings.removeAll(oldpv);
        return remainings;
    }

}
