package snob.simulation.customobservers;

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
import peersim.core.Node;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.snob2.Datastore;
import snob.simulation.snob2.QuerySnob;
import snob.simulation.snob2.Snob;
import snob.simulation.spray.Spray;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.exit;

public class SprayObserver implements ObserverProgram {

    private HashMap<Long, Set<Long>> observed = new HashMap<>();
    private HashMap<Long, Long> finished = new HashMap<>();

    public SprayObserver(String prefix) {}

    @Override
    public void tick(long currentTick, DictGraph observer) {
        double meanpvsize = observer.meanPartialViewSize();

        for(int i = 0; i< Network.size(); ++i) {
            Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;
            if(!observed.containsKey(Network.get(i).getID())) {
                observed.put(Network.get(i).getID(), new LinkedHashSet<>());
                observed.get(Network.get(i).getID()).add(Network.get(i).getID());
            }
            if(observed.get(Network.get(i).getID()).size() == Network.size()) {
                finished.put(Network.get(i).getID(), currentTick);
            } else {
                for (Node peer : spray.getPeers(Integer.MAX_VALUE)) {
                    observed.get(Network.get(i).getID()).add(peer.getID());
                }
            }
        }
        Stat stat = new Stat(observed, finished);
        String[] result = {
                String.valueOf(currentTick),
                String.valueOf(stat.meanFinished),
                String.valueOf(stat.meanObserved),
                String.valueOf(stat.minObserved),
                String.valueOf(stat.maxObserved)
        };
        System.out.println(String.join(",", result));
        if(finished.size() == Network.size()) {
            System.err.println("Finish: " + finished.size());
            System.err.println("Observed: " + stat.meanObserved);
            System.err.println("Mean finished: " + stat.meanFinished);
            exit(0);
        }
    }

    class Stat {
        Stat(HashMap<Long, Set<Long>> observed, HashMap<Long, Long> finished) {
            meanObserved = 0;
            minObserved = -1;
            for (Set<Long> value : observed.values()) {
                meanObserved += value.size();
                if(minObserved == -1) {
                    minObserved = value.size();
                } else if(value.size() < minObserved){
                    minObserved = value.size();
                }
                if(maxObserved == -1) {
                    maxObserved = value.size();
                } else if(value.size() > maxObserved){
                    maxObserved = value.size();
                }
            }
            meanFinished = 0;
            for (Long value : finished.values()) {
                meanFinished += value;
            }
            meanObserved = meanObserved/Network.size();
            meanFinished = meanFinished/Network.size();

        }
        public double meanObserved;
        public double meanFinished;
        public double minObserved;
        public double maxObserved;

    }

    private double meanFinished() {
        double mean = 0;
        for (Long value : finished.values()) {
            mean += value;
        }
        return mean/Network.size();
    }

    private double meanObservedPv() {
        double mean = 0;
        for (Set<Long> value : observed.values()) {
            mean += value.size();
        }
        return mean/Network.size();
    }

    private double minObservedPv() {
        double min = -1;
        for (Set<Long> value : observed.values()) {
            if(min == -1) {
                min = value.size();
            } else if(value.size() < min){
                min = value.size();
            }
        }
        return min;
    }

    private double maxObservedPv() {
        double max = -1;
        for (Set<Long> value : observed.values()) {
            if(max == -1) {
                max = value.size();
            } else if(value.size() > max){
                max = value.size();
            }
        }
        return max;
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }
}
