package snob.simulation.snob2;

import org.apache.commons.math3.special.Gamma;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Var;
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
    private final int begin = 50;
    private int query = 73;
    private int replicate;
    private int queries;
    private boolean initialized = false;
    private Map<Integer, Integer> fullmeshcompleted = new LinkedHashMap<>();
    private Map<Integer, Integer> completed = new LinkedHashMap<>();
    private Map<Integer, Integer> seenfinished = new LinkedHashMap<>();

    public SnobObserver(String prefix) {
        try {
            this.query = Configuration.getInt(prefix + ".querytoreplicate", 73);
            this.replicate = Configuration.getInt(prefix + ".replicate", 50);
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
        int networksize = Network.size();
        try {
            long messages = 0;
            long triplesback = 0;
            int meancompleted = 0;
            int meanfullmeshcompleted = 0;
            int meanfinish = 0;
            for (int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                messages += snob.messages;
                triplesback += snob.tripleResponses;
                if (snob.profile.has_query) {
                    QuerySnob query = snob.profile.query;
                    if (query != null) {
                        List<QuerySolution> res = query.getResults();
                        int cpt = res.size();
                        long localcomp = 0;
                        if (cpt > query.cardinality) {
                            throw new Exception("pipeline " + query.query + " gives more results than expected...");
                        } else if (cpt == 0 && query.cardinality == 0) {
                            localcomp = 100;
                        } else if (cpt == 0 && query.cardinality > 0) {
                            localcomp = 0;
                        } else if (cpt > 0 && query.cardinality > 0) {
                            localcomp = (cpt / query.cardinality) * 100;
                        } else {
                            throw new Exception("case not handled.... cpt=" + cpt + " cardinality= " + query.cardinality);
                        }

                        if (!fullmeshcompleted.containsKey(snob.id) && snob.fullmesh.size() == (replicate - 1)) {
                            fullmeshcompleted.put(snob.id, current);
                        }

                        if (!completed.containsKey(snob.id) && localcomp == 100) {
                            completed.put(snob.id, current);
                        }
                        if (!seenfinished.containsKey(snob.id) && snob.profile.query.globalseen == networksize && query.isFinished()) {
                            seenfinished.put(snob.id, current);
                        }

                        // compute average
                        if (seenfinished.containsKey(snob.id)) {
                            meanfinish += seenfinished.get(snob.id);
                        }
                        if (completed.containsKey(snob.id)) {
                            meancompleted += completed.get(snob.id);
                        }
                        if (fullmeshcompleted.containsKey(snob.id)) {
                            meanfullmeshcompleted += fullmeshcompleted.get(snob.id);
                        }
                    }
                }
            }

            if (completed.size() > 0) {
                meancompleted = meancompleted / completed.size();
            } else {
                meancompleted = 0;
            }
            if (seenfinished.size() > 0) {
                meanfinish = meanfinish / seenfinished.size();
            } else {
                meanfinish = 0;
            }
            if (fullmeshcompleted.size() > 0) {
                meanfullmeshcompleted = meanfullmeshcompleted / fullmeshcompleted.size();
            } else {
                meanfullmeshcompleted = 0;
            }

            if(triplesback != 0) {
                triplesback = triplesback / this.queries;
            }

            if(messages != 0) {
                messages = messages / this.queries;
            }

            double approximation = (Network.size() * (Math.log(Network.size() - this.queries) + Gamma.GAMMA)) / (this.queries * Snob.pick);
            String res = observer.size()
                    + ", " + this.queries
                    + ", " + Snob.pick
                    + ", " + Snob.c
                    + ", " + Snob.l
                    + ", " + approximation
                    + ", " + meanfinish
                    + ", " + meancompleted
                    + ", " + meanfullmeshcompleted
                    + ", " + triplesback
                    + ", " + messages
                    + ", " + seenfinished.size();
            if (seenfinished.size() == this.queries) {
                System.out.println(res);
                exit(0);
            }
        } catch (Exception e) {
            System.err.println("ERROR:" + e);
            e.printStackTrace();
        }
    }

    public void init(DictGraph observer) {
        Datastore d = new Datastore();
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
        for (Object filename : filenames) {
            d.update(filename.toString());
        }

        Vector<Snob> peers = new Vector();
        for (int i = 0; i < networksize; ++i) {
            Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
            peers.add(snob);
        }

        // now create the construct query
        Triple spo = new Triple(Var.alloc("s"), Var.alloc("p"), Var.alloc("o"));
        List<Triple> result = d.getTriplesMatchingTriplePatternAsList(spo);
        Collections.shuffle(result);
        int k = 0;
        Iterator<Triple> it = result.iterator();
        while (it.hasNext()) {
            Triple triple = it.next();
            List<Triple> list = new LinkedList<>();
            list.add(triple);
            peers.get(k % peers.size()).profile.datastore.insertTriples(list);
            k++;
        }
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
        JSONObject queryToreplicate = finalQueries.get(this.query); // finalQueries.get((int) Math.floor(Math.random() * queriesDiseasome.size()));
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
