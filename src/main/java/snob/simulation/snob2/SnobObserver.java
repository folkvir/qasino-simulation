package snob.simulation.snob2;

import org.apache.jena.query.QuerySolution;
import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.snob2.data.IBFStrata;

import java.util.Iterator;
import java.util.List;

public class SnobObserver implements ObserverProgram {
    public SnobObserver(String p) {
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if (currentTick > 0) {
            // hack to get the proper pid.... fix it for a proper version
            int networksize = Network.size();
            try {
                Snob snob_default = (Snob) observer.nodes.get(Network.get(0).getID()).pss;

                long completeness = 0;
                double completenessinresults;
                long messages = 0;
                double totalreceivedresults = 0;
                double totalcardinality = 0;
                int triplesback = 0;
                double triplebackmean = 0;
                int estimateErrores = 0;
                double peerSeenMean = 0;
                int peerHigherThanPeers = 0;
                System.err.println("Network size: " + networksize);
                for (int i = 0; i < networksize; ++i) {
                    Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                    messages += snob.messages;
                    triplesback += snob.tripleResponses;
                    peerSeenMean += snob.profile.alreadySeen.size();
                    if(snob.profile.alreadySeen.size() >= 200) {
                        peerHigherThanPeers++;
                    }

                    Iterator<IBFStrata> it = snob.profile.strata.values().iterator();
                    while (it.hasNext()) {
                        estimateErrores += it.next().estimateErrored;
                    }

                    QuerySnob query = snob.profile.query;
                    if (query != null) {
                        List<QuerySolution> res = query.getResults();
                        int cpt = res.size();
                        System.err.printf("[Peer-%d] has a query with %d/%d results. (see %d distinct peers) %n", snob.id, cpt, query.cardinality, snob.profile.alreadySeen.size());
                        snob.profile.alreadySeen.forEach(id -> {
                            System.err.printf("[%s/%f]", id, cpt/query.cardinality);
                        });
                        System.err.println();
                        totalreceivedresults += cpt;
                        totalcardinality += query.cardinality;
                        if (cpt > query.cardinality) {
                            throw new Exception("pipeline " + query.query + " gives more results than expected...");
                        } else if (cpt == 0 && query.cardinality == 0) {
                            completeness += 100;
                        } else if (cpt == 0 && query.cardinality > 0) {
                            completeness += 0;
                        } else if (cpt > 0 && query.cardinality > 0) {
                            completeness += (cpt / query.cardinality) * 100;
                        } else {
                            throw new Exception("case not handled.... cpt=" + cpt + " cardinality= " + query.cardinality);
                        }
                    }
                }

                if (totalcardinality == 0) {
                    System.err.println("totalcardinality=0");
                    completenessinresults = 0;
                } else {
                    completenessinresults = (totalreceivedresults) / (totalcardinality) * 100;
                }
                triplebackmean = triplesback / snob_default.profile.qlimit;
                completeness = completeness / snob_default.profile.qlimit;
                peerSeenMean = peerSeenMean / snob_default.profile.qlimit;
                System.err.println("Global Completeness in the network: " + completeness + "% (" + snob_default.profile.qlimit + "," + networksize + ")");
                System.err.println("Global Completeness (in results) in the network: " + completenessinresults + "% (" + totalreceivedresults + "," + totalcardinality + ")");
                System.err.println("Number of messages in the network: " + messages);

                String res = currentTick
                        + ", " + observer.size()
                        + ", " + observer.meanPartialViewSize()
                        + ", " + snob_default.getPeers(Integer.MAX_VALUE).size()
                        + ", " + ((snob_default.son) ? snob_default.getSonPeers(Integer.MAX_VALUE).size() : 0)
                        + ", " + completeness
                        + ", " + messages
                        + ", " + totalreceivedresults
                        + ", " + totalcardinality
                        + ", " + completenessinresults
                        + ", " + triplesback
                        + ", " + triplebackmean
                        + ", " + estimateErrores
                        + ", " + peerSeenMean
                        + ", " + peerHigherThanPeers ;
                System.out.println(res);
                System.err.println(res);
            } catch (Exception e) {
                System.err.println("ERROR:" + e);
            }
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }
}
