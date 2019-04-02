package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import snob.simulation.rps.ARandomPeerSamplingProtocol;
import snob.simulation.rps.IMessage;
import snob.simulation.rps.IRandomPeerSampling;
import snob.simulation.snob2.data.IBFStrata;
import snob.simulation.spray.Spray;
import snob.simulation.spray.SprayMessage;
import snob.simulation.spray.SprayPartialView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SnobSpray extends ARandomPeerSamplingProtocol implements
        IRandomPeerSampling {
    private static final String PAR_TRAFFIC = "traffic"; // minimization of the traffic
    public static boolean traffic;
    public static boolean start = false;
    private static int snobs = 0;
    public final int id = SnobSpray.snobs++;
    public int shuffle = 0;
    // Profile of the peer
    public Profile profile = new Profile();
    public double messages = 0;
    public long tripleResponses = 0;
    public int observed = 0;

    // #A no configuration needed, everything is adaptive
    // #B no values from the configuration file of peersim
    // #C local variables
    public SprayPartialView partialView;

    public Long oldest = null;
    public List<Long> previousSample = new ArrayList<>();
    public List<Node> previousSampleNode = new ArrayList<>();
    public List<Long> previousPartialview = new ArrayList<>();
    public List<Node> previousPartialviewNode = new ArrayList<>();
    /**
     * Constructor of the Spray instance
     *
     * @param prefix the peersim configuration
     */
    public SnobSpray(String prefix) {
        super(prefix);
        SnobSpray.traffic = Configuration.getBoolean(prefix + "." + PAR_TRAFFIC);
        this.profile = new Profile();
        this.partialView = new SprayPartialView();
    }

    public SnobSpray() {
        this.partialView = new SprayPartialView();
    }

    public static SnobSpray fromNodeToSnob(Node node) {
        return (SnobSpray) node.getProtocol(ARandomPeerSamplingProtocol.pid);
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
            SnobSpray qSpray = (SnobSpray) q
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

                if(start) {
                    shuffle++;
                    // APPLY SNOB NOW
                    List<Node> currentPartialview = this.getPeers(Integer.MAX_VALUE);
                    List<Node> peers = computeRealNewPeers(this.node.getID(), this.oldest, previousPartialviewNode, previousSampleNode, currentPartialview);
                    if (profile.has_query && !profile.query.terminated) {
                        // 1 - send tpqs to neighbours and receive responses
                        for (Node node_rps : peers) {
                            observed++;
                            // System.err.println(this.id +" exchange with " + fromNodeToSnob(node_rps).id);
                            this.exchangeTriplePatterns(fromNodeToSnob(node_rps), true);
                            if (fromNodeToSnob(node_rps).profile.has_query) {
                                fromNodeToSnob(node_rps).exchangeTriplePatterns(this, true);
                            }
                        }
                        profile.execute();
                        // test if the query is terminated or not, WHEN WE SAW N PEEES
                        boolean shouldstop = profile.query.isFinished();
//                  // test whether the query is finished or not using a probabilistic criterion
//                  boolean shouldstop = profile.query.probabilisticIsFinished(0.99, this.shuffle);


                        if(shouldstop){
                            this.profile.stop();
                        }
                    }
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
            SnobSpray contactSpray = (SnobSpray) contact.getProtocol(Spray.pid);
            this.partialView.clear();
            this.partialView.addNeighbor(contact);
            contactSpray.onSubscription(this.node);
        }
        this.isUp = true;
    }

    public void onSubscription(Node origin) {
        List<Node> aliveNeighbors = this.getAliveNeighbors();
        for (Node neighbor : aliveNeighbors) {
            SnobSpray neighborSpray = (SnobSpray) neighbor.getProtocol(Spray.pid);
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
            SnobSpray sprayClone = new SnobSpray();
            sprayClone.partialView = (SprayPartialView) this.partialView.clone();
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

    /**
     * Exchange Triple patterns with Invertible bloom filters associated with.
     * In return the other peers send us missing triples for each triple pattern.
     * See onTpqs(...) function
     *
     * @param remote
     */
    protected void exchangeTriplePatterns(SnobSpray remote, boolean share) {
        this.profile.query.patterns.forEach(pattern -> {
            if (traffic) {
                exchangeTriplesFromPatternUsingIbf(remote, pattern, share);
            } else {
                this.messages++;
                List<Triple> list = remote.profile.datastore.getTriplesMatchingTriplePatternAsList(pattern);
                tripleResponses += list.size();
                this.profile.insertTriplesWithList(pattern, list);
            }

            this.profile.query.addAlreadySeen(pattern, remote.id, this.id);
            if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
                int before = this.profile.query.alreadySeen.get(pattern).size();
                this.profile.query.mergeAlreadySeen(pattern, remote.profile.query.alreadySeen.get(pattern));
                int after = this.profile.query.alreadySeen.get(pattern).size();
            }
        });
    }

    /**
     * Exchange triple pattern using Invertible bloom filter, and share data if necessary
     *
     * @param remote
     * @param pattern
     */
    public void exchangeTriplesFromPatternUsingIbf(SnobSpray remote, Triple pattern, boolean share) {
        // send the ibf to remote peer with the pattern
        if (!remote.profile.has_query || (remote.profile.has_query && !remote.profile.query.patterns.contains(pattern))) {
            // if remote does not have a query, get triple pattern, get ibf on this triple pattern, set reconciliation.
            List<Triple> computed = remote.profile.datastore.getTriplesMatchingTriplePatternAsList(pattern);
            if (computed.size() == 0) {
                this.messages++; // at least one message
            } else {
                IBFStrata remoteIbf = IBFStrata.createIBFFromTriples(computed);
                IBFStrata.Result res = this.profile.query.strata.get(pattern).difference(remoteIbf);
                if(res.messagessent == 0) {
                    this.messages++;
                } else {
                    this.messages += res.messagessent;
                }
                int t = this.profile.insertTriplesWithList(pattern, res.missing);
                this.tripleResponses += t;
            }
        } else {
            // the remote peer has the pattern
            IBFStrata.Result res = this.profile.query.strata.get(pattern).difference(remote.profile.query.strata.get(pattern));
            if(res.messagessent == 0) {
                this.messages++;
            } else {
                this.messages += res.messagessent;
            }
            int t = this.profile.insertTriplesWithList(pattern, res.missing);
            this.tripleResponses += t;
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
