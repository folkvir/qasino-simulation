package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import peersim.config.Configuration;
import peersim.core.Node;
import snob.simulation.cyclon.Cyclon;
import snob.simulation.rps.ARandomPeerSamplingProtocol;
import snob.simulation.snob2.data.IBFStrata;

import java.util.List;

public class SnobCyclon  extends Cyclon {
    private static final String PAR_TRAFFIC = "traffic"; // minimization of the traffic
    public static boolean traffic;

    public static boolean start = false;

    public static int pick = 5;
    private static int snobs = 0;

    public final int id = SnobCyclon.snobs++;

    public int shuffle = 0;

    // Profile of the peer
    public Profile profile;
    public double messages = 0;
    public long tripleResponses = 0;

    public SnobCyclon(String prefix) {
        super(prefix);
        Snob.pick = Configuration.getInt(prefix + ".pick");
        Snob.traffic = Configuration.getBoolean(prefix + "." + PAR_TRAFFIC);
        this.profile = new Profile();
    }

    public static SnobCyclon fromNodeToSnob(Node node) {
        return (SnobCyclon) node.getProtocol(ARandomPeerSamplingProtocol.pid);
    }

    public void periodicCall() {
        super.periodicCall();

        if (profile.has_query && !profile.query.terminated) {
            // 1 - send tpqs to neighbours and receive responses
            for (Node node_rps : this.getPeers(pick)) {
                // System.err.println(this.id +" exchange with " + fromNodeToSnob(node_rps).id);
                this.exchangeTriplePatterns(fromNodeToSnob(node_rps), true);
                if (fromNodeToSnob(node_rps).profile.has_query) {
                    fromNodeToSnob(node_rps).exchangeTriplePatterns(this, true);
                }
            }
            profile.execute();
            // test if the query is terminated or not, WHEN WE SAW N PEEES
            boolean shouldstop = profile.query.isFinished();
//            // test whether the query is finished or not using a probabilistic criterion
//            boolean shouldstop = profile.query.probabilisticIsFinished(0.99, this.shuffle);


            if(shouldstop){
                this.profile.stop();
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
    protected void exchangeTriplePatterns(SnobCyclon remote, boolean share) {
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
    public void exchangeTriplesFromPatternUsingIbf(SnobCyclon remote, Triple pattern, boolean share) {
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
}
