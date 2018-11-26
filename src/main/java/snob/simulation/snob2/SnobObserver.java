package snob.simulation.snob2;

import org.apache.jena.query.QuerySolution;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import peersim.config.Configuration;
import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.snob2.data.IBFStrata;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.exit;

public class SnobObserver implements ObserverProgram {
    private int qlimit; // limit of queries loaded in the network
    private int dlimit; // limit of fragments loaded in the network
    private int replicate;

    //    private int pid;
//    private static final String PAR_PROTOCOL = "protocol";
    public SnobObserver(String prefix) {
        System.err.println("Initializing: " + prefix);
        try {
            // this.pid = Configuration.lookupPid(Configuration.getString(prefix + ".snob"));
            this.qlimit = Configuration.getInt(prefix + ".qlimit", -1);
            System.err.println("Setting the query limit to: " + this.qlimit);
            this.replicate = Configuration.getInt(prefix + ".replicate", 50);
            System.err.println("Setting the replicate factor to (%): " + this.replicate);
            this.dlimit = Configuration.getInt(prefix + ".dlimit", -1);
            System.err.println("Setting the fragments limit to: " + this.dlimit);
        } catch (Exception e) {
            System.err.println("Cant find any query limit: setting value to unlimited: " + e);
        }
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if (currentTick == 0) {
            init(observer);
        } else {
            observe(currentTick, observer);
        }
    }

    public void observe(long currentTick, DictGraph observer) {
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
                peerSeenMean += snob.profile.query.globalseen;
                if (snob.profile.query.globalseen >= networksize) {
                    peerHigherThanPeers++;
                }

                Iterator<IBFStrata> it = snob.profile.query.strata.values().iterator();
                while (it.hasNext()) {
                    estimateErrores += it.next().estimateErrored;
                }

                QuerySnob query = snob.profile.query;
                if (query != null) {
                    List<QuerySolution> res = query.getResults();
                    int cpt = res.size();
                    // System.err.printf("[Peer-%d] has a query with %d/%d results. (see %d distinct peers) %n", snob.id, cpt, query.cardinality, snob.profile.query.globalseen);
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
                    + ", " + peerHigherThanPeers;
            System.out.println(res);
            System.err.println(res);

            if (peerHigherThanPeers == Network.size()) {
                exit(0);
            }
        } catch (Exception e) {
            System.err.println("ERROR:" + e);
            e.printStackTrace();
        }
    }

    public void init(DictGraph observer) {
        // hack to get the proper pid.... fix it for a proper version
        int networksize = Network.size();
        System.err.println("[INIT:SNOB] Initialized data for: " + networksize + " peers..." + observer.nodes.size());
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        System.err.println(System.getProperty("user.dir"));
        String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
        String linkedmdb = System.getProperty("user.dir") + "/datasets/data/linkedmdb/fragments/";
        String linkedmdbQuery = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries_jena_generated.json";

        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        try (Stream<Path> paths = Files.walk(Paths.get(linkedmdb))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        System.err.println("[INIT:SNOB] Number of fragments to load: " + filenames.size());

        Vector<Snob> peers = new Vector();
        for (int i = 0; i < networksize; ++i) {
            Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
            peers.add(snob);
        }
        int pickedElement = 0;
        int peersPicked = 0;
        this.dlimit = (this.dlimit == -1) ? filenames.size() : this.dlimit;
        while (pickedElement < this.dlimit && pickedElement < filenames.size()) {
            System.err.println("Loading data into peer:" + peersPicked);
            System.err.println(filenames.get(pickedElement).toString());
            peers.get(peersPicked).profile.datastore.update(filenames.get(pickedElement).toString());
            peersPicked++;
            if (peersPicked > peers.size() - 1) peersPicked = 0;
            pickedElement++;
        }

        // diseasome queries
        JSONParser parser = new JSONParser();
        Vector<JSONObject> queriesDiseasome = new Vector();
        Vector<JSONObject> queriesLinkedmdb = new Vector();
        try (Reader is = new FileReader(diseasomeQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.stream().forEach((q) -> {
                JSONObject j = (JSONObject) q;
                queriesDiseasome.add(j);
            });

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        // linkedmdb queries
        try (Reader is = new FileReader(linkedmdbQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.stream().forEach((q) -> {
                JSONObject j = (JSONObject) q;
                queriesLinkedmdb.add(j);
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        // create a vector containing all queries where queries are inserted one after the other respectively from each dataset
        Vector<JSONObject> finalQueries = new Vector();
        int j = 0;
        for (int i = 0; i < queriesDiseasome.size(); i++) {
            finalQueries.add(queriesDiseasome.get(i));
            if (j < queriesLinkedmdb.size()) {
                finalQueries.add(queriesLinkedmdb.get(j));
            }
            j++;
        }

        // now check the replicate factor and replicate the first query in this vector
        JSONObject queryToreplicate = finalQueries.get(0);
        double numberOfReplicatedQueries = Math.floor(peers.size() * replicate / 100);
        System.err.printf("Replicating %f times the query: %s", numberOfReplicatedQueries, queryToreplicate.get("query").toString());
        for (int i = 0; i < numberOfReplicatedQueries; ++i) {
            finalQueries.set(i, (JSONObject) queryToreplicate.clone());
        }


        // set queries on each peer
        int pickedQuery = 0;
        peersPicked = 0;
        List<JSONObject> queries = new ArrayList<>();
        for (int i = 0; i < peers.size(); ++i) {
            queries.add(finalQueries.get(i));
        }

        // shuffle queries =)
        Collections.shuffle(queries);
        int max = 0;
        if (qlimit == -1) {
            max = peers.size();
        } else {
            max = qlimit;
        }
        for (int i = 0; i < networksize; ++i) {
            Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
            snob.profile.qlimit = max;
            snob.profile.replicate = this.replicate;
            if (max != 0) {
                JSONObject query = queries.get(i);
                snob.profile.update((String) query.get("query"), (long) query.get("card"));
                max--;
            }
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }
}
