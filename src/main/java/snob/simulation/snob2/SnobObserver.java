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
    private final int begin = 0;
    private int replicate;
    private int queries;
    private boolean initialized = false;
    private Map<Integer, Integer> seenfinished = new LinkedHashMap<>();
    private int firstq = -1;
    private double firstqcompleteness = 0;
    private int firstqmessages = 0;
    private long firstqtriplesback = 0;

    public SnobObserver(String prefix) {
        // System.err.println("Initializing: " + prefix);
        try {
            this.replicate = Configuration.getInt(prefix + ".replicate", 50);
            // System.err.println("Setting the replicate factor to (%): " + this.replicate);
        } catch (Exception e) {
            System.err.println("Cant find any query limit: setting value to unlimited: " + e);
        }
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if (currentTick == begin) {
            init(observer);
            initialized = true;
            Snob.start = true;
        } else {
            if (initialized) observe(currentTick, observer);
        }
    }

    public void observe(long currentTick, DictGraph observer) {
        int current = (int) currentTick - begin;
        // hack to get the proper pid.... fix it for a proper version
        int networksize = Network.size();
        try {
            Snob snob_default = (Snob) observer.nodes.get(Network.get(0).getID()).pss;
            long completeness = 0;
            double completenessinresults;
            long messages = 0;
            long firstqmessagesfullmesh = 0;
            double totalreceivedresults = 0;
            double totalcardinality = 0;
            int triplesback = 0;
            double triplebackmean = 0;
            double peerSeenMean = 0;
            int firstqrpssize = 0;
            int firstqfullmeshsize = 0;
            int firstqnbtpqs = 0;

            System.err.println("Network size: " + networksize);
            for (int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                messages += snob.messages;
                triplesback += snob.tripleResponses;
                if (snob.profile.has_query) {
                    peerSeenMean += snob.profile.query.globalseen;
                    if (!seenfinished.containsKey(snob.id) && snob.profile.query.globalseen == networksize) {
                        if (firstq == -1) {
                            firstq = current;
                            firstqrpssize = snob.getPeers(Integer.MAX_VALUE).size();
                            firstqfullmeshsize = snob.fullmesh.size();
                            firstqcompleteness = snob.profile.query.getResults().size() / snob.profile.query.cardinality * 100;
                            firstqmessages = snob.messages;
                            firstqtriplesback = snob.tripleResponses;
                            firstqmessagesfullmesh = snob.messagesFullmesh;
                            firstqnbtpqs = snob.profile.query.patterns.size();
                        }
                        seenfinished.put(snob.id, current);
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
            }

            if (totalcardinality == 0) {
                completenessinresults = 0;
            } else {
                completenessinresults = (totalreceivedresults) / (totalcardinality) * 100;
            }


            triplebackmean = triplesback / this.queries;
            if (completeness == 0) {
                completeness = 0;
            } else {
                completeness = completeness / this.queries;
            }
            peerSeenMean = peerSeenMean / this.queries;

            System.err.println("Global Completeness in the network: " + completeness + "% (" + this.queries + "," + networksize + ")");
            System.err.println("Global Completeness (in results) in the network: " + completenessinresults + "% (" + totalreceivedresults + "," + totalcardinality + ")");

            double meanQN = 0;
            for (Map.Entry<Integer, Integer> entry : seenfinished.entrySet()) {
                meanQN += entry.getValue();
            }
            if (meanQN != 0) meanQN = meanQN / seenfinished.size();

            double approximation = Math.floor(Network.size() * Math.log(Network.size()) / (this.queries * Snob.c)) + 1;
            double ratio = meanQN / approximation;


            if (firstq != -1) {
                double minmessages = firstqnbtpqs * (firstq - 1) * firstqrpssize;
                double maxmessages = 0;
                if (Snob.son) {
                    if (Snob.traffic) {
                        maxmessages = firstqnbtpqs * ((firstq - 1) * (2 * firstqrpssize * (this.queries - 1)));
                    } else {
                        maxmessages = firstqnbtpqs * ((firstq - 1) * (firstqrpssize * (this.queries - 1)));
                    }
                } else {
                    if (Snob.traffic) {
                        maxmessages = firstqnbtpqs * ((firstq - 1) * 2 * firstqrpssize);
                    } else {
                        maxmessages = firstqnbtpqs * ((firstq - 1) * firstqrpssize);
                    }
                }
                System.err.println("Max allowed number of messages for firstq = " + maxmessages);
                double meanmessages = firstqmessages;
                System.err.println("Mean number of messages =" + meanmessages);

                String res = observer.size()
                        + ", " + this.queries
                        + ", " + firstqrpssize
                        + ", " + firstqfullmeshsize
                        + ", " + observer.meanClusterCoefficient()
                        + ", " + firstq
                        + ", " + firstqcompleteness
                        + ", " + firstqmessages
                        + ", " + maxmessages
                        + ", " + minmessages
                        + ", " + firstqmessagesfullmesh
                        + ", " + meanmessages
                        + ", " + firstqtriplesback
                        + ", " + approximation
                        + ", " + ratio;
                System.out.println(res);
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
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
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
        while (pickedElement < filenames.size() && pickedElement < filenames.size()) {
            peers.get(peersPicked).profile.datastore.update(filenames.get(pickedElement).toString());
            peersPicked++;
            if (peersPicked > peers.size() - 1) peersPicked = 0;
            pickedElement++;
        }

        // diseasome queries
        JSONParser parser = new JSONParser();
        Vector<JSONObject> queriesDiseasome = new Vector();
        try (Reader is = new FileReader(diseasomeQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.stream().forEach((q) -> {
                JSONObject j = (JSONObject) q;
                queriesDiseasome.add(j);
            });

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        // create a vector containing all queries where queries are inserted one after the other respectively from each dataset
        Vector<JSONObject> finalQueries = new Vector();
        for (int i = 0; i < queriesDiseasome.size(); i++) {
            finalQueries.add(queriesDiseasome.get(i));
        }

        // now check the replicate factor and replicate a random query.
        JSONObject queryToreplicate = finalQueries.get(73); // finalQueries.get((int) Math.floor(Math.random() * queriesDiseasome.size()));
        int numberOfReplicatedQueries = this.replicate;

        // pick peer that will receive queries
        List<Snob> nodes = new ArrayList<>();
        while (nodes.size() != numberOfReplicatedQueries) {
            int rn = (int) Math.floor(Math.random() * Network.size());
            Snob n = (Snob) observer.nodes.get(Network.get(rn).getID()).pss;
            if (!nodes.contains(n)) nodes.add(n);
        }

        this.queries = numberOfReplicatedQueries;
        for (int i = 0; i < nodes.size(); ++i) {
            Snob snob = nodes.get(i);
            snob.profile.replicate = numberOfReplicatedQueries;
            snob.profile.update((String) queryToreplicate.get("query"), (long) queryToreplicate.get("card"));
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }
}
