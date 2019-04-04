package snob.simulation.customobservers;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import peersim.config.Configuration;
import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.snob2.Datastore;
import snob.simulation.snob2.SnobSpray;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.exit;

public class SnobSprayObserver implements ObserverProgram {
    private final int begin = 50;
    private int query = 73;

    private JSONObject queryToreplicate = null;


    // can be "lasvegas", end when all have seen all, or "montecarlo", if montecarlo, proportion=0.999999999 until the end of the experiment
    private String stopcond = "lasvegas";
    private double proportion = 0.999999999;
    private double montecarlostop = 0;
    private int replicate;
    private int queries;
    private boolean initialized = false;

    private Map<Long, SnobSpray> collaborativepeers = new LinkedHashMap<>();

    public SnobSprayObserver(String prefix) {
        try {
            this.stopcond = Configuration.getString(prefix + ".stopcond", "lasvegas");
            this.query = Configuration.getInt(prefix + ".querytoreplicate", 73);
            this.replicate = Configuration.getInt(prefix + ".replicate", 50);
        } catch (Exception e) {
            System.err.println("Cant find any query limit: setting value to unlimited: " + e);
        }
        this.montecarlostop = Network.size() * Math.log((1 / (1 - proportion)));

    }

    public static Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if (json != null) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keySet().iterator();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if (currentTick == begin) {
            init(observer);
            System.err.println(queryToreplicate);
            initialized = true;
            SnobSpray.start = true;
        } else {
            if (initialized) observe(currentTick, observer);
        }
    }

    public void observe(long currentTick, DictGraph observer) {
        // always pick the first collaborative peer.
        Map.Entry<Long, SnobSpray> entry = collaborativepeers.entrySet().iterator().next();
        SnobSpray peer = entry.getValue();
        Map<String, Object> data = jsonToMap((JSONObject) queryToreplicate.get("patterns"));
        long sumRequired = 0;
        long sumAcquired = 0;
        for (Map.Entry<String, Object> d : data.entrySet()) {
            sumRequired += (long) d.getValue();
        }
        for (Triple p : peer.profile.query.plan.inserted.keySet()) {
            sumAcquired += peer.profile.query.plan.inserted.get(p);
        }

        double seen = 0;
        for (Triple triple : peer.profile.query.alreadySeen.keySet()) {
            seen += peer.profile.query.alreadySeen.get(triple).size();
        }
        seen = seen / peer.profile.query.alreadySeen.keySet().size();
        System.out.println(String.join(",", new String[]{
                String.valueOf(peer.node.getID()),
                String.valueOf(peer.shuffle),
                String.valueOf(peer.observed),
                String.valueOf(seen),
                String.valueOf(peer.crdt.sum()),
                String.valueOf(peer.messages),
                String.valueOf(peer.tripleResponses),
                String.valueOf(peer.profile.inserted),
                String.valueOf(sumAcquired),
                String.valueOf(sumRequired),
                String.valueOf(sumAcquired / sumRequired),
                String.valueOf(peer.profile.datastore.inserted),
                String.valueOf(peer.profile.query.getResults().size()),
                String.valueOf(peer.profile.query.cardinality),
                String.valueOf(Network.size()),
                String.valueOf(peer.getPeers(Integer.MAX_VALUE).size()),
                String.valueOf(replicate),
                String.valueOf(SnobSpray.traffic),
                String.valueOf(query)
        }));

        boolean shouldexit = true;
        for (Map.Entry<Long, SnobSpray> end : collaborativepeers.entrySet()) {
            // should stop the query
            switch (stopcond) {
                case "lasvegas":
                    lasVegasCondition(end.getValue());
                    break;
                case "montecarlo":
                    monteCarloCondition(end.getValue());
                    break;
                default:
                    System.err.println("No termination defined! switch to las vegas termination");
                    lasVegasCondition(end.getValue());
            }
            // should exi
            shouldexit = shouldexit && end.getValue().profile.query.terminated;
        }
        if (shouldexit) {
            exit(0);
        }
    }

    private void monteCarloCondition(SnobSpray peer) {
        if (peer.profile.has_query && peer.crdt.sum() > montecarlostop) {
            peer.profile.stop();
        }
    }

    private void lasVegasCondition(SnobSpray peer) {
        if (peer.profile.has_query && peer.profile.query.isFinished()) {
            peer.profile.stop();
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }

    public void init(DictGraph observer) {
        Datastore d = new Datastore();
        // hack to get the proper pid.... fix it for a proper version
        int networksize = Network.size();
        System.err.println("[INIT:SNOB-SPRAY] Initialized data for: " + networksize + " peers..." + observer.nodes.size());
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        System.err.println(System.getProperty("user.dir"));
        String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        System.err.println("[INIT:SNOB-SPRAY] Number of fragments to load: " + filenames.size());
        for (Object filename : filenames) {
            d.update(filename.toString());
        }

        Vector<SnobSpray> peers = new Vector();
        for (int i = 0; i < networksize; ++i) {
            SnobSpray snob = (SnobSpray) (observer.nodes.get(Network.get(i).getID()).pss);
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
        this.queryToreplicate = queryToreplicate;

        // pick peer that will receive queries
        List<SnobSpray> nodes = new ArrayList<>();
        while (nodes.size() != numberOfReplicatedQueries) {
            int rn = (int) Math.floor(Math.random() * Network.size());
            SnobSpray n = (SnobSpray) observer.nodes.get(Network.get(rn).getID()).pss;
            if (!nodes.contains(n)) nodes.add(n);
        }

        this.queries = numberOfReplicatedQueries;
        for (int i = 0; i < nodes.size(); ++i) {
            SnobSpray snob = nodes.get(i);
            collaborativepeers.put(snob.node.getID(), snob);
            System.err.println("Add a new collaborative peer: " + snob.node.getID());
            snob.profile.replicate = numberOfReplicatedQueries;
            snob.profile.update((String) queryToreplicate.get("query"), (long) queryToreplicate.get("card"));
        }
    }
}
