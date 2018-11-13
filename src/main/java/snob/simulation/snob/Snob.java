package snob.simulation.snob;

import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import snob.simulation.rps.ARandomPeerSamplingProtocol;
import snob.simulation.rps.IMessage;
import snob.simulation.rps.IRandomPeerSampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The Snob protocol
 */
public class Snob extends ARandomPeerSamplingProtocol implements IRandomPeerSampling {
	// #A the names of the parameters in the configuration file of peersim
	private static final String PAR_C = "c"; // max partial view size
    private static final String PAR_L = "l"; // shuffle size
    private static final String PAR_SON_C = "sonc"; // max partial view size
    private static final String PAR_SON_L = "sonl"; // shuffle size
    private static final String PAR_SON = "son"; // enable son or not


    // #B the values from the configuration file of peersim
	public static int c;
    public static int l;
    public static boolean son;
    public static int sonc;
    public static int sonl;

	// Profile of the peer
	public Profile profile;

	// #C local variables
    public SnobPartialView partialView;
    public SonPartialView sonPartialView;
    public static int RND_WALK = 5;
    public String prefix;
    public int messages = 0;

	/**
	 * Construction of a Snob instance, By default it is a Cyclon implementation wihtout using the overlay
	 * 
	 * @param prefix
	 *            the peersim configuration
	 */
	public Snob(String prefix) {
		super(prefix);
        this.prefix = prefix;
		Snob.c = Configuration.getInt(prefix + "." + PAR_C);
        Snob.l = Configuration.getInt(prefix + "." + PAR_L);
        Snob.sonc = Configuration.getInt(prefix + "." + PAR_SON_C);
        Snob.sonl = Configuration.getInt(prefix + "." + PAR_SON_L);
        Snob.son = Configuration.getBoolean(prefix + "." + PAR_SON);
		this.partialView = new SnobPartialView(Snob.c, Snob.l);
        if(Snob.son) {
            this.sonPartialView = new SonPartialView(Snob.sonc, Snob.sonl);
        }
        try {
            this.profile = new Profile();
            System.err.println("Creating the profile...");
        } catch (Exception e) {
            System.err.println(e);
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
		this.messages = 0; // reset the number of messages

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
				this.partialView.mergeSample(this.node, q, samplePrime, sample,true);
			} else {
				// #B if the chosen peer is dead, remove it from the view
				this.partialView.removeNode(q);
			}
		}

        // do the periodic shuffling for the Semantic Overlay if enabled
        if(Snob.son && this.isUp() && this.sonPartialView.size() > 0){
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
                this.sonPartialView.mergeSample(this, qSon, samplePrime, sample,true);
            } else {
                // #B if the chosen peer is dead, remove it from the view
                this.sonPartialView.removeNode(qSon);
            }
        }

        // -------- QUERY EXECUTION MODEL -------

        // 1 - send tpqs to neighbours and receive responses
        List<Node> rps_neigh = this.getPeers(1000000);
        for (Node node1 : rps_neigh) {
        	if(this.profile.tpqs.size() > 0) {
				Snob snob = (Snob) node1.getProtocol(ARandomPeerSamplingProtocol.pid);
				IMessage received = snob.onTpqs(this.node, new SnobTpqsMessage(this.profile.tpqs));
				// 2 - insert responses into our datastore
				this.profile.datastore.insertTriples((List<Triple>) received.getPayload());
				this.messages++;
			}
        }
		if(Snob.son && this.isUp() && this.sonPartialView.size() > 0){
            List<Node> son_neigh = this.getSonPeers(1000000);
			for (Node node1 : son_neigh) {
				if(this.profile.tpqs.size() > 0) {
					Snob snob = (Snob) node1.getProtocol(ARandomPeerSamplingProtocol.pid);
					IMessage received = snob.onTpqs(this.node, new SnobTpqsMessage(this.profile.tpqs));
					// 2 - insert responses into our datastore
					this.profile.datastore.insertTriples((List<Triple>) received.getPayload());
					this.messages++;
				}
			}
		}

		// 3 - perform the execution of all queries
        profile.executeAll();
	}

    public IMessage onTpqs(Node origin, IMessage message) {
        Object receivedTriples = (List<Triple>) message.getPayload();
        List<Triple> result = new ArrayList<>();
        Iterator<Triple> it = ((List) receivedTriples).iterator();
        while(it.hasNext()) {
            Iterator<Triple> matchingTriples = this.profile.datastore.getTriplesMatchingTriplePattern(it.next());
            while(matchingTriples.hasNext()) {
                result.add(matchingTriples.next());
            }
        }
        return new SnobTpqsMessage(result);
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
            if(Snob.son) {
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
            if(Snob.son) s.sonPartialView = (SonPartialView) this.sonPartialView.clone();
			return s;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Perform a random walk in the network at a depth set by ttl (time-to-live)
	 * 
	 * @param origin
	 *            the subscribing peer
	 * @param current
	 *            the current peer that either accept the subcription or
	 *            forwards it
	 * @param ttl
	 *            the current time-to-live before the subscription gets accepted
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
						if(Snob.son) {
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
	public boolean addNeighbor(Node peer) {
	    if(Snob.son) return this.partialView.addNeighbor(peer) && this.sonPartialView.addNeighbor(peer);
		return  this.partialView.addNeighbor(peer);
	}

}
