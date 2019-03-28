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

    private HashMap<Long, Set<Long>> observed = new LinkedHashMap<>();

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
            for (Node peer : spray.getPeers(Integer.MAX_VALUE)) {
                observed.get(Network.get(i).getID()).add(peer.getID());
            }
        }
        String[] result = {
                String.valueOf(currentTick),
                String.valueOf(meanObservedPv()),
                String.valueOf(minObservedPv()),
                String.valueOf(maxObservedPv())
        };
        System.out.println(String.join(",", result));
        if(meanObservedPv() == Network.size()) {
            exit(0);
        }
    }

    private double meanObservedPv() {
        double mean = 0;
        for (Set<Long> value : observed.values()) {
            mean += value.size();
        }
        return mean/observed.size();
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
