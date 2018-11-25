package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import snob.simulation.rps.ARandomPeerSamplingProtocol;
import snob.simulation.rps.IMessage;
import snob.simulation.rps.IRandomPeerSampling;
import snob.simulation.snob2.messages.SnobMessage;

import java.util.*;

/**
 * The Snob protocol
 */
public class Snob extends ARandomPeerSamplingProtocol implements IRandomPeerSampling {
    // #A the names of the parameters in the configuration file of peersim
    private static final String PAR_C = "c"; // max partial view size
    private static final String PAR_L = "l"; // shuffle size
    private static final String PAR_SON_C = "sonc"; // shuffle size on the rps
    private static final String PAR_SON_L = "sonl"; // shuffle size on the son
    private static final String PAR_SON = "son"; // enable son or not
    private static final String PAR_TRAFFIC = "traffic"; // minimization of the traffic
    // #B the values from the configuration file of peersim
    public static int c;
    public static int l;
    public static boolean son;
    public static int sonc;
    public static int sonl;
    public static int RND_WALK = 5;
    public static boolean traffic;
    private static int snobs = 0;
    public final int id = Snob.snobs++;
    // Profile of the peer
    public Profile profile;
    // #C local variables
    public SnobPartialView partialView;
    public SonPartialView sonPartialView;
    public String prefix;
    // instrumentations
    public int messages = 0;
    public long tripleResponses = 0;

    /**
     * Construction of a Snob instance, By default it is a Cyclon implementation wihtout using the overlay
     *
     * @param prefix the peersim configuration
     */
    public Snob(String prefix) {
        super(prefix);
        this.prefix = prefix;
        Snob.c = Configuration.getInt(prefix + "." + PAR_C);
        Snob.l = Configuration.getInt(prefix + "." + PAR_L);
        Snob.sonc = Configuration.getInt(prefix + "." + PAR_SON_C);
        Snob.sonl = Configuration.getInt(prefix + "." + PAR_SON_L);
        Snob.son = Configuration.getBoolean(prefix + "." + PAR_SON);
        Snob.traffic = Configuration.getBoolean(prefix + "." + PAR_TRAFFIC);
        this.partialView = new SnobPartialView(Snob.c, Snob.l);
        if (Snob.son) {
            this.sonPartialView = new SonPartialView(Snob.sonc, Snob.sonl);
        }
        try {
            this.profile = new Profile();
            System.err.println("Creating the profile...");
        } catch (Exception e) {
            System.err.println(e);
        }
        System.err.printf("[%d/%d] Snob peer initialized. %n", id, Network.size());
    }

    /**
     * Perform a random walk in the network at a depth set by ttl (time-to-live)
     *
     * @param origin  the subscribing peer
     * @param current the current peer that either accept the subcription or
     *                forwards it
     * @param ttl     the current time-to-live before the subscription gets accepted
     */
    private static void randomWalk(Node origin, Node current, int ttl) {
        final Snob originSnob = (Snob) origin.getProtocol(Snob.pid);
        final Snob currentSnob = (Snob) current.getProtocol(Snob.pid);
        List<Node> aliveNeighbors = currentSnob.getAliveNeighbors();
        ttl -= 1;
        // #A if the receiving peer has neighbors in its partial view
        if (aliveNeighbors.size() > 0) {
            // #A1 if the ttl is greater than 0, continue the random walk
            if (ttl > 0) {
                final Node next = aliveNeighbors.get(CommonState.r
                        .nextInt(aliveNeighbors.size()));
                randomWalk(origin, next, ttl);
            } else {
                // #B if the ttl is greater than 0 or the partial view is empty,
                // then
                // accept the subscription and stop forwarding it
                if (origin.getID() != current.getID()) {
                    Iterator<Node> iPeers = currentSnob.getPeers(1)
                            .iterator();
                    if (iPeers.hasNext()) {
                        Node chosen = iPeers.next();
                        currentSnob.partialView.removeNode(chosen);
                        originSnob.partialView.addNeighbor(chosen);
                        if (Snob.son) {
                            currentSnob.sonPartialView.removeNode(chosen);
                            originSnob.sonPartialView.addNeighbor(chosen);
                        }
                    }
                    currentSnob.addNeighbor(origin);
                }
            }
        }
    }

    @Override
    protected boolean pFail(List<Node> path) {
        // the probability is constant since the number of hops to establish
        // a connection is constant
        double pf = 1 - Math.pow(1 - ARandomPeerSamplingProtocol.fail, 6);
        return CommonState.r.nextDouble() < pf;
    }

    public void periodicCall() {
        messages = 0;
        tripleResponses = 0;
        // do the periodic shuffling of the rps
        if (this.isUp() && this.partialView.size() > 0) {
            // do the periodic shuffling for Cyclon
            this.partialView.incrementAge();
            Node q = this.partialView.getOldest();
            Snob qSnob = (Snob) q.getProtocol(ARandomPeerSamplingProtocol.pid);
            if (qSnob.isUp() && !this.pFail(null)) {
                // #A if the chosen peer is alive, initiate the exchange
                List<Node> sample = this.partialView.getSample(this.node, q, true);
                sample.add(this.node);
                IMessage received = qSnob.onPeriodicCall(this.node, new SnobMessage(sample));
                List<Node> samplePrime = (List<Node>) received.getPayload();
                this.partialView.mergeSample(this.node, q, samplePrime, sample, true);
            } else {
                // #B if the chosen peer is dead, remove it from the view
                this.partialView.removeNode(q);
            }
        }

        // do the periodic shuffling for the Semantic Overlay if enabled
        if (Snob.son && this.isUp() && this.sonPartialView.size() > 0) {
            this.sonPartialView.incrementAge();
            Node qSon = this.sonPartialView.getOldest();
            Snob qSonSnob = (Snob) qSon.getProtocol(ARandomPeerSamplingProtocol.pid);
            if (qSonSnob.isUp() && !this.pFail(null)) {
                // #A if the chosen peer is alive, initiate the exchange
                List<Node> sample = this.sonPartialView.getSample(this, qSon,
                        true);
                sample.add(this.node);
                IMessage received = qSonSnob.onPeriodicCallSon(this.node, new SnobMessage(sample));
                List<Node> samplePrime = (List<Node>) received.getPayload();
                this.sonPartialView.mergeSample(this, qSon, samplePrime, sample, true);
            } else {
                // #B if the chosen peer is dead, remove it from the view
                this.sonPartialView.removeNode(qSon);
            }
        }

        // -------- QUERY EXECUTION MODEL -------
        if (profile.has_query && !profile.query.terminated) {
            // 1 - send tpqs to neighbours and receive responses
            List<Node> rps_neigh = this.getPeers(1000000);
            for (Node node_rps : rps_neigh) {
                if (this.profile.has_query && this.profile.query.patterns.size() > 0) {
                    Snob snob = (Snob) node_rps.getProtocol(ARandomPeerSamplingProtocol.pid);
                    this.exchangeTriplePatterns(snob);
                }
            }
            if (Snob.son && this.isUp() && this.sonPartialView.size() > 0) {
                List<Node> son_neigh = this.getSonPeers(1000000);
                for (Node node_son : son_neigh) {
                    if (this.profile.has_query && this.profile.query.patterns.size() > 0) {
                        Snob snob = (Snob) node_son.getProtocol(ARandomPeerSamplingProtocol.pid);
                        this.exchangeTriplePatterns(snob);
                    }
                }
            }
            profile.execute();
        }
    }

    /**
     * Exchange Triple patterns with Invertible bloom filters associated with.
     * In return the other peers send us missing triples for each triple pattern.
     * See onTpqs(...) function
     *
     * @param remote
     */
    private void exchangeTriplePatterns(Snob remote) {
        if (!this.profile.query.terminated) {
            System.err.printf("Transferring data from %s to %s...", remote.id, this.id);
            Map<Triple, Iterator<Triple>> result = new HashMap<>();
            this.profile.query.patterns.forEach(pattern -> {
                if (this.traffic) {
                    List<Triple> l = this.profile.query.strata.get(pattern).exchange(pattern, remote);
                    // if(l.size() > 0) System.err.printf("[%s] just receive %d triples from %s...%n", this.id, l.size(), remote.id);
                    result.put(pattern, l.iterator());
                } else {
                    result.put(pattern, remote.profile.datastore.getTriplesMatchingTriplePattern(pattern));
                }
                this.profile.query.addAlreadySeen(pattern, remote.id, this.id);
                if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
                    this.profile.query.mergeAlreadySeen(pattern, remote.profile.query.alreadySeen.get(pattern));
                }
            });
            int insertedtriples = this.profile.insertTriples(result, traffic);
            tripleResponses += insertedtriples;
            // System.err.printf("[%s] Inserting %d triples from %s%n ", this.id, insertedtriples, remote.id);
            this.messages++;

            // test if the query is terminated or not
            if (this.profile.query.globalseen == Network.size()) {
                this.profile.query.stop();
            }
            System.err.printf(" *end* %n");
        }
    }

    public IMessage onPeriodicCall(Node origin, IMessage message) {
        List<Node> samplePrime = this.partialView.getSample(this.node, origin,
                false);
        this.partialView.mergeSample(this.node, origin,
                (List<Node>) message.getPayload(), samplePrime, false);

        return new SnobMessage(samplePrime);
    }

    public IMessage onPeriodicCallSon(Node origin, IMessage message) {
        List<Node> samplePrime = this.sonPartialView.getSample(this, origin,
                false);
        this.sonPartialView.mergeSample(this, origin,
                (List<Node>) message.getPayload(), samplePrime, false);
        return new SnobMessage(samplePrime);
    }

    public void join(Node joiner, Node contact) {
        if (this.node == null) { // lazy loading of the node identity
            this.node = joiner;
        }
        if (contact != null) { // the very first join does not have any contact
            Snob contactSnob = (Snob) contact.getProtocol(Snob.pid);
            this.partialView.clear();
            this.partialView.addNeighbor(contact);
            if (Snob.son) {
                this.sonPartialView.clear();
                this.sonPartialView.addNeighbor(contact);
            }
            contactSnob.onSubscription(this.node);
        }
        this.isUp = true;
    }

    public void onSubscription(Node origin) {
        List<Node> aliveNeighbors = this.getAliveNeighbors();
        Collections.shuffle(aliveNeighbors, CommonState.r);
        int nbRndWalk = Math.min(Snob.c - 1, aliveNeighbors.size());

        for (int i = 0; i < nbRndWalk; ++i) {
            randomWalk(origin, aliveNeighbors.get(i), Snob.RND_WALK);
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

    public List<Node> getSonPeers(int k) {
        return this.sonPartialView.getPeers(k);
    }

    @Override
    public IRandomPeerSampling clone() {
        try {
            Snob s = new Snob(this.prefix);
            s.partialView = (SnobPartialView) this.partialView.clone();
            if (Snob.son) s.sonPartialView = (SonPartialView) this.sonPartialView.clone();
            return s;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addNeighbor(Node peer) {
        if (Snob.son) return this.partialView.addNeighbor(peer) && this.sonPartialView.addNeighbor(peer);
        return this.partialView.addNeighbor(peer);
    }

}
