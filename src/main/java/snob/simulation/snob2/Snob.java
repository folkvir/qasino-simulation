package snob.simulation.snob2;

import com.google.common.collect.Lists;
import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import snob.simulation.rps.ARandomPeerSamplingProtocol;
import snob.simulation.rps.IMessage;
import snob.simulation.rps.IRandomPeerSampling;
import snob.simulation.snob2.data.IBFStrata;
import snob.simulation.snob2.data.Strata.Cell;
import snob.simulation.snob2.data.Strata.StrataEstimator;
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
    public static boolean start = false;
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
    public static int pick = 5;
    // Profile of the peer
    public Profile profile;
    // #C local variables
    public SnobPartialView partialView;
    public SonPartialView sonPartialView;
    public String prefix;
    // instrumentations
    public int messages = 0;
    public int messagesFullmesh = 0;
    public long tripleResponses = 0;
    public Set<Node> fullmesh = new LinkedHashSet<Node>();

    // for the IKnowYou perriodic call
    public int step = 0;
    public int by = 1;
    public boolean finish = false;
    /**
     * Construction of a Snob instance, By default it is a Cyclon implementation wihtout using the overlay
     *
     * @param prefix the peersim configuration
     */
    public Snob(String prefix) {
        super(prefix);
        this.prefix = prefix;
        Snob.pick = Configuration.getInt(prefix + ".pick");
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
            // System.err.println("Creating the profile...");
        } catch (Exception e) {
            System.err.println(e);
        }
        // System.err.printf("[%d/%d] Snob peer initialized. %n", id, Network.size());
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
        periodicCyclonCall();
    }

    public static Snob fromNodeToSnob(Node node ) {
        return (Snob) node.getProtocol(ARandomPeerSamplingProtocol.pid);
    }

    /**
     * Construct the neighborhood using the RPS
     */
    public void periodicRandomGraphCall() {
        if (this.isUp()) {
            this.partialView.clear();
            List<Node> rps = new LinkedList<>();
            while (rps.size() != c) {
                int rn = (int) Math.floor(Math.random() * Network.size());
                Node randomNode = Network.get(rn);
                if (!rps.contains(randomNode) && !randomNode.equals(this.node)) {
                    rps.add(randomNode);
                    this.partialView.addNeighbor(randomNode);
                }
            }
            // son
            constructFullmesh(rps);
            // -------- QUERY EXECUTION MODEL -------
            if (start && profile.has_query && !profile.query.terminated) {
                // 1 - send tpqs to neighbours and receive responses
                List<Node> rps_neigh = rps;
                for (Node node_rps : rps_neigh) {
                    this.exchangeTriplePatterns((Snob) node_rps.getProtocol(ARandomPeerSamplingProtocol.pid));
                }
                profile.execute();
                // test if the query is terminated or not
                if (this.profile.query.globalseen == Network.size()) {
                    this.profile.query.stop();
                }
            }
        }
    }

    /**
     * Construct the fullmesh for the specified list of nodes
     * @param rps
     */
    public void constructFullmesh(List<Node> rps) {
        rps.forEach(peer -> {
            if (!fullmesh.contains(peer)) {
                Snob remote = fromNodeToSnob(peer);
                if (remote.profile.has_query && this.profile.has_query && this.profile.query.query.equals(remote.profile.query.query)) {
                    // merge fullmesh
                    // LinkedHashSet<Integer> seen = new LinkedHashSet<>();
                    fullmesh.addAll(remote.fullmesh);
                    fullmesh.add(this.node);
                    fullmesh.add(remote.node);

                    // synchronize data everywhere
                    // synchronize fullmesh data
                    // firstly pull
                    for (Node n : fullmesh) {
                        if(!n.equals(this.node)) this.exchangeTriplePatterns(fromNodeToSnob(n));
                    }
                    for (Node n : fullmesh) {
                        if(!n.equals(this.node)) fromNodeToSnob(n).exchangeTriplePatterns(fromNodeToSnob(this.node));
                    }


                    // seen.addAll(this.profile.query.seen);
                    LinkedHashSet<Node> us = new LinkedHashSet<>();
                    for (Node p : fullmesh) {
                        // seen.addAll(fromNodeToSnob(p).profile.query.seen);
                        if(p.equals(this.node)) {
                            us.addAll(fullmesh);
                            us.remove(p);
                        } else {
                            fromNodeToSnob(p).fullmesh.addAll(fullmesh);
                            fromNodeToSnob(p).fullmesh.remove(p);
                        }
                    }
//                    for (Node node : fullmesh) {
//                        fromNodeToSnob(node).profile.query.seen.addAll(seen);
//                    }
                    // System.err.println("us: "+ us + "fullmesh: " + fullmesh);
                    fullmesh = us;
                    // System.err.println("us: "+ us + "fullmesh: " + fullmesh);
                    // end merge fullmerge
                }
            }
        });
    }

    /**
     * Periodic call for the Cyclon Network
     */
    public void periodicCyclonCall() {
//        messages = 0;
//        tripleResponses = 0;
        // do the periodic shuffling of the rps
        if (this.isUp() && this.partialView.size() > 0) {
            // do the periodic shuffling for Cyclon
            this.partialView.incrementAge();
            Node q = this.partialView.getOldest();
            Snob qSnob = fromNodeToSnob(q);
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
        if (start && Snob.son && this.isUp()) {
            // list the rps, check for Snob that are not in our fullmesh son, add it
            List<Node> rps = this.getPeers(this.pick);
            constructFullmesh(rps);
        }
        // -------- QUERY EXECUTION MODEL -------
        if (start && profile.has_query && !profile.query.terminated) {
            // 1 - send tpqs to neighbours and receive responses
            List<Node> rps_neigh = this.getPeers(this.pick);
            for (Node node_rps : rps_neigh) {
                // System.err.println(this.id +" exchange with " + fromNodeToSnob(node_rps).id);
                this.exchangeTriplePatterns(fromNodeToSnob(node_rps));
            }
            profile.execute();
            // test if the query is terminated or not
            if (this.profile.query.globalseen == Network.size()) {
                this.profile.query.stop();
            }
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
        // System.err.printf("[peer-%d/query-%d]Transferring data from %s to %s... %n", this.id, profile.query.qid, remote.id, this.id);
        // System.err.println("** before: " + this.profile.query.seen);
        this.profile.query.patterns.forEach(pattern -> {
            if (traffic) {
                exchangeTriplesFromPatternUsingIbf(remote, pattern, true);
            } else {
                this.messages++;
                Iterator<Triple> it = remote.profile.datastore.getTriplesMatchingTriplePattern(pattern);
                tripleResponses += this.profile.insertTriples(pattern, it, false);
                if (Snob.son) {
                    this.shareTriples(Lists.newArrayList(it), pattern);
                }
            }
            this.profile.query.addAlreadySeen(pattern, remote.id, this.id);
            if (remote.profile.has_query && remote.profile.query.patterns.contains(pattern)) {
                this.profile.query.mergeAlreadySeen(pattern, remote.profile.query.alreadySeen.get(pattern));
            }
            // System.err.println("** after, for the pattern: " + this.profile.query.alreadySeen.get(pattern));
            // System.err.println("** after global seen: " + this.profile.query.alreadySeen.get(pattern));
        });
        // System.err.println("** FINAL = " + this.profile.query.seen);
    }

    /**
     * Share triples with the fullmesh
     * @param list
     * @param pattern
     */
    public void shareTriples(List<Triple> list, Triple pattern) {
        if (this.isUp && Snob.son) {
            // System.err.println("share triples");
            if (list.size() != 0) {
                // System.err.println("We have " + list.size() + "triples to share.");
                // share knowledge into the full mesh network
                fullmesh.forEach(node -> {
                    this.messagesFullmesh++;
                    this.messages++;
                    Snob remote = fromNodeToSnob(node);
                    IBFStrata localibf = IBFStrata.createIBFFromTriples(list);
                    // directly send the ibf
                    // once receive proceed to the set difference.
                    IBFStrata remoteibf = remote.profile.query.strata.get(pattern);
                    Cell[] cells = remoteibf.ibf.subtract(localibf.ibf.getCells()).clone();
                    List<Integer>[] difference = remoteibf.ibf.decode(cells);
                    if (difference == null) {
                        // means that we cannot make the difference
                        // send a message to get all triples matching the pattern.
                        this.messagesFullmesh++;
                        remote.tripleResponses += remote.profile.insertTriples(pattern, list.iterator(), true);
                    } else {
                        this.messagesFullmesh++;
                        this.messages++;
                        // send the missing keys to the remote pair. Answer with missing triples
                        List<Integer> plus = difference[0];
                        Iterator<Integer> miss = difference[1].iterator();
                        List<Triple> triples = new LinkedList<>();
                        while (miss.hasNext()) {
                            int hash = miss.next();
                            triples.add(localibf.data.get(hash));
                        }
                        remote.tripleResponses += remote.profile.insertTriples(pattern, triples.iterator(), true);
                    }
                });
            }
        }
    }

    /**
     * Exchange triple pattern using Invertible bloom filter, and share data if necessary
     * @param remote
     * @param pattern
     * @param traffic
     */
    public void exchangeTriplesFromPatternUsingIbf(Snob remote, Triple pattern, boolean traffic) {
        // System.err.println("exchange using ibf");
        IBFStrata localibf = this.profile.query.strata.get(pattern);
        this.messages++;
        // send the ibf to remote peer with the pattern
        if (!remote.profile.has_query || (remote.profile.has_query && !remote.profile.query.patterns.contains(pattern))) {
            // if remote does not have a query, get triple pattern, get ibf on this triple pattern, set reconciliation.
            Iterator<Triple> it = remote.profile.datastore.getTriplesMatchingTriplePattern(pattern);
            List<Triple> res = new LinkedList<>();
            while (it.hasNext()) {
                res.add(it.next());
            }
            if (res.size() == 0) {
                // send back an empty list.
                this.tripleResponses += 0;
            } else {
                IBFStrata remoteIbf = IBFStrata.createIBFFromTriples(res);
                StrataEstimator localstrat = localibf.getEstimator();
                int diffSize = remoteIbf.getEstimator().decode(localstrat);
                if (diffSize == 0) {
                    // return empty list, nothing to do.
                    return;
                }
                if (diffSize * 2 > IBFStrata.ibfSize) {
                    // directly send triples, because too big.
                    this.tripleResponses += this.profile.insertTriples(pattern, res.iterator(), true);
                    shareTriples(res, pattern);
                } else {
                    // (from the remote) send the ibf to us
                    // once receive proceed to the set difference.
                    Cell[] cells = localibf.ibf.subtract(remoteIbf.ibf.getCells()).clone();
                    List<Integer>[] difference = localibf.ibf.decode(cells);
                    if (difference == null) {
                        // means that we cannot make the difference
                        // send a message to get all triples matching the pattern.
                        this.messages++;
                        this.tripleResponses += this.profile.insertTriples(pattern, res.iterator(), true);
                        shareTriples(res, pattern);
                    } else {
                        this.messages++;
                        // send the missing keys to the remote pair. Answer with missing triples
                        Iterator<Integer> miss = difference[1].iterator();
                        List<Triple> triples = new LinkedList<>();
                        while (miss.hasNext()) {
                            int hash = miss.next();
                            triples.add(remoteIbf.data.get(hash));
                        }
                        this.tripleResponses += this.profile.insertTriples(pattern, triples.iterator(), true);
                        shareTriples(res, pattern);
                    }
                }
            }
        } else {
            // the remote peer has the pattern
            IBFStrata remoteIbf = remote.profile.query.strata.get(pattern);
            StrataEstimator localstrat = localibf.getEstimator();
            int diffSize = remoteIbf.getEstimator().decode(localstrat);
            List<Triple> data = remoteIbf.getTriples();
            if (diffSize * 2 > IBFStrata.ibfSize) {
                // directly send triples, because too big.
                this.tripleResponses += this.profile.insertTriples(pattern, data.iterator(), true);
                shareTriples(data, pattern);
            } else {
                // (from the remote) send the ibf to us
                // once receive proceed to the set difference.
                Cell[] cells = localibf.ibf.subtract(remoteIbf.ibf.getCells()).clone();
                List<Integer>[] difference = localibf.ibf.decode(cells);
                if (difference == null) {
                    // means that we cannot make the difference
                    // send a message to get all triples matching the pattern.
                    this.messages++;
                    this.tripleResponses += this.profile.insertTriples(pattern, data.iterator(), true);
                    shareTriples(data, pattern);
                } else {
                    this.messages++;
                    // send the missing keys to the remote pair. Answer with missing triples
                    Iterator<Integer> miss = difference[1].iterator();
                    List<Triple> triples = new LinkedList<>();
                    while (miss.hasNext()) {
                        int hash = miss.next();
                        triples.add(remoteIbf.data.get(hash));
                    }
                    this.tripleResponses += this.profile.insertTriples(pattern, triples.iterator(), true);
                    shareTriples(triples, pattern);
                }
            }
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
