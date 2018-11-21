package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
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
import java.rmi.ServerError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;


public class SnobInit implements ObserverProgram {
    private int qlimit; // limit of queries loaded in the network
    private int dlimit; // limit of fragments loaded in the network
    private int replicate;
    public SnobInit(String prefix) {
        System.err.println("Initializing: " + prefix);
        try {
            this.qlimit = Configuration.getInt(prefix + ".qlimit", -1);
            System.err.println("Setting the replicate factor to (%): " + this.qlimit);
            this.replicate = Configuration.getInt(prefix + ".replicate", 50);
            System.err.println("Setting the query limit to: " + this.qlimit);
            this.dlimit = Configuration.getInt(prefix + ".dlimit", -1);
            System.err.println("Setting the fragments limit to: " + this.dlimit);
        } catch (Exception e) {
            System.err.println("Cant find any query limit: setting value to unlimited: " + e);
        }
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if (currentTick == 1) {
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
                if(j < queriesLinkedmdb.size()){
                    finalQueries.add(queriesLinkedmdb.get(j));
                }
                j++;
            }

            // now check the replicate factor and replicate the first query in this vector
            JSONObject queryToreplicate = finalQueries.get(0);
            double numberOfReplicatedQueries = Math.floor(peers.size() * replicate / 100);
            System.err.printf("Replicating %f times the query: %s", numberOfReplicatedQueries, queryToreplicate.get("query").toString());
            for(int i = 0; i < numberOfReplicatedQueries; ++i) {
                finalQueries.set(i, (JSONObject) queryToreplicate.clone());
            }



            // set queries on each peer
            int pickedQuery = 0;
            peersPicked = 0;
            List<JSONObject> queries = new ArrayList<>();
            for(int i= 0;i < peers.size(); ++i) {
                queries.add(finalQueries.get(i));
            }

            // shuffle queries =)
            Collections.shuffle(queries);
            int max = 0;
            if(qlimit == -1) {
                max = peers.size();
            } else {
                max = qlimit;
            }
            for (int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                snob.profile.qlimit = max;
                snob.profile.replicate = this.replicate;
                if(max != 0) {
                    JSONObject query = queries.get(i);
                    snob.profile.update((String) query.get("query"), (long) query.get("card"));
                    max--;
                }

            }

            /*// collect all possible triple patterns available in initialized profiles...
            List<Triple> patterns = new ArrayList();
            System.err.println("Initialize all globa IBFs on all peers (could be very long to to this, please wait.)");
            for (int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                // collect all patterns
                snob.profile.patterns.forEach(pattern -> {
                    if(!patterns.contains(pattern)) {
                        patterns.add(pattern);
                    }
                });
            }
            // now initialize IBFs
            for (int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                snob.profile.initializeGlobalIBF(patterns);
            }*/
        }
    }


    @Override
    public void onLastTick(DictGraph observer) {

    }
}
