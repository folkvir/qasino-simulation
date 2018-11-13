package snob.simulation.snob;

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
import java.util.Collections;
import java.util.Vector;
import java.util.stream.Stream;


public class SnobInit implements ObserverProgram {
    private int qlimit; // limit of query loaded in the network
    private int dlimit; // limit of fragments loaded in the network


    public SnobInit(String prefix) {
        System.err.println("Initializing: " + prefix);
        try {
            this.qlimit = Configuration.getInt(prefix + ".qlimit", -1);
            System.err.println("Setting the query limit to: " + this.qlimit);
            this.dlimit = Configuration.getInt(prefix + ".dlimit", -1);
            System.err.println("Setting the fragments limit to: " + this.dlimit);
        } catch (Exception e) {
            System.err.println("Cant find any query limit: setting value to unlimited: " + e);
        }
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if(currentTick == 1) {
            // hack to get the proper pid.... fix it for a proper version
            int networksize = Network.size();
            System.err.println("[INIT:SNOB] Initialized data for: " + networksize + " peers..." + observer.nodes.size());
            String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
            String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
            String linkedmdb = System.getProperty("user.dir") + "/datasets/data/linkedmdb/fragments/";
            String linkedmdbQuery = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries_jena_generated.json";

            Vector filenames = new Vector();
            try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
                paths.filter(Files::isRegularFile).forEach((fileName)->filenames.add(fileName));
            } catch(IOException e) {
                System.err.println(e.toString());
            }
            try (Stream<Path> paths = Files.walk(Paths.get(linkedmdb))) {
                paths.filter(Files::isRegularFile).forEach((fileName)->filenames.add(fileName));
            } catch(IOException e) {
                System.err.println(e.toString());
            }
            System.err.println("[INIT:SNOB] Number of fragments to load: " + filenames.size());

            Vector<Snob> peers = new Vector();
            for(int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                peers.add(snob);
            }
            int pickedElement = 0;
            int peersPicked = 0;
            this.dlimit = (this.dlimit == -1)?filenames.size():this.dlimit;
            while(pickedElement < this.dlimit && pickedElement < filenames.size()) {
                System.err.println("Loading data into peer:" + peersPicked);
                peers.get(peersPicked).profile.datastore.update(filenames.get(pickedElement).toString());
                peersPicked++;
                if(peersPicked > peers.size() - 1) peersPicked = 0;
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

            queriesDiseasome.addAll(queriesLinkedmdb);
            Collections.shuffle(queriesDiseasome);

            int pickedQuery = 0;
            peersPicked = 0;
            this.qlimit = (this.qlimit == -1)?queriesDiseasome.size():this.qlimit;
            for(int i = 0; i < networksize; ++i) {
                Snob snob = (Snob) observer.nodes.get(Network.get(i).getID()).pss;
                System.err.println(i);
                snob.profile.qlimit = this.qlimit;
            }
            System.err.println("Number of queries to load: [" + this.qlimit + "/" + queriesDiseasome.size() + "]...");
            while(pickedQuery < this.qlimit && pickedQuery < queriesDiseasome.size()) {
                System.err.println("Loading query into peer:" + peersPicked);
                peers.get(peersPicked).profile.update(queriesDiseasome.get(pickedQuery).get("query").toString(), (long) queriesDiseasome.get(pickedQuery).get("card"));
                System.err.println("Number of queries for peer-" + peersPicked + ": " + peers.get(peersPicked).profile.queries.size());
                peersPicked++;
                if(peersPicked > peers.size() - 1) peersPicked = 0;
                pickedQuery++;
            }
        }
    }


    @Override
    public void onLastTick(DictGraph observer) {

    }
}
