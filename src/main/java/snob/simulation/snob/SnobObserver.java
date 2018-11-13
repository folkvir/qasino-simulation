package snob.simulation.snob;

import org.apache.jena.query.ResultSet;
import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;

import java.util.Iterator;
import java.util.UUID;

public class SnobObserver implements ObserverProgram {
    public SnobObserver(String p) {}
    @Override
    public void tick(long currentTick, DictGraph observer) {
        if(currentTick > 0) {
            // hack to get the proper pid.... fix it for a proper version
            int networksize = Network.size();
            try {
                Snob snob_default = (Snob) observer.nodes.get(Network.get(0).getID()).pss;

                long completeness = 0;
                long messages = 0;
                for(int i = 0; i < networksize; ++i) {
                    Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                    messages += snob.messages;
                    Iterator<UUID> keys = snob.profile.queries.keySet().iterator();
                    // System.err.println("Number of queries for peer-"+Network.get(i).getID() + ": " +snob.profile.results.size());
                    if(keys.hasNext()) {
                        UUID key = keys.next();
                        QuerySnob query = snob.profile.queries.get(key);
                        ResultSet res = query.results;
                        if(res != null) {
                            long cpt = 0;
                            while(res.hasNext()) {
                                res.next();
                                cpt++;
                            }
                            if (cpt != 0 && query.cardinality != 0) {
                              completeness += (cpt) / (query.cardinality) * 100;
                            } else if (cpt == 0 && query.cardinality == 0) {
                              completeness += 100;
                            } else {
                              completeness += 0;
                            }
                        }
                    }
                }
                completeness = completeness / snob_default.profile.qlimit;
                System.err.println("Global Completeness in the network: " + completeness + "% ("+ snob_default.profile.qlimit + "," + networksize + ")");
                System.err.println("Number of messages in the network: " + messages);
                if (snob_default.son) {
                    System.out.println(currentTick
                            + ", " + observer.size()
                            + ", " + observer.countPartialViewsWithDuplicates()
                            + ", " + observer.meanPartialViewSize()
                            + ", " + snob_default.getPeers(Integer.MAX_VALUE).size()
                            + ", " + snob_default.getSonPeers(Integer.MAX_VALUE).size()
                            + ", " + completeness
                            + ", " + messages);
                } else {
                    System.out.println(currentTick
                            + ", " + observer.size()
                            + ", " + observer.countPartialViewsWithDuplicates()
                            + ", " + observer.meanPartialViewSize()
                            + ", " + snob_default.getPeers(Integer.MAX_VALUE).size()
                            + ", " + 0
                            + ", " + completeness
                            + ", " + messages);
                }
            } catch(Exception e) {
                System.err.println("ERROR:" + e);
            }
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }
}
