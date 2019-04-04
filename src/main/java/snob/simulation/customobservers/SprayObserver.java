package snob.simulation.customobservers;

import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.spray.Spray;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SprayObserver implements ObserverProgram {
    // with one round memory
    private HashMap<Long, List<Long>> lastround = new HashMap<>();
    private HashMap<Long, Integer> lastRoundMemory = new HashMap<>();
    // without memory
    private HashMap<Long, Integer> withoutMemory = new HashMap<>();
    // with full memory
    private HashMap<Long, Set<Long>> observed = new HashMap<>();
    private HashMap<Long, Long> finished = new HashMap<>();

    public SprayObserver(String prefix) {
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        double meanpvsize = observer.meanPartialViewSize();

        for (int i = 0; i < Network.size(); ++i) {
            Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;
            List<Long> currentPartialview = spray.getPeers(Integer.MAX_VALUE).parallelStream().map(node -> {
                return observer.nodes.get(node.getID()).id;
            }).collect(Collectors.toList());


            List<Long> newPeers = this.computeRealNewPeers(spray.node.getID(), spray.oldest, spray.previousPartialview, spray.previousSample, currentPartialview);
            // System.err.println("Observer:(" + spray.node.getID() + ")_(" + spray.oldest + ")" + spray.previousPartialview + "_" + spray.previousSample + "_" + currentPartialview + "_newPeers:" + newPeers);
            // real new peers

            if (!withoutMemory.containsKey(Network.get(i).getID())) {
                withoutMemory.put(Network.get(i).getID(), newPeers.size());
            } else {
                withoutMemory.put(Network.get(i).getID(), withoutMemory.get(Network.get(i).getID()) + newPeers.size());
            }
//            if(!lastround.containsKey(Network.get(i).getID())) {
//                lastround.put(Network.get(i).getID(), observer.nodes.get(Network.get(i).getID()).neighbors);
//                lastRoundMemory.put(Network.get(i).getID(), 0);
//            }
//
//
//            for (Node peer : spray.getPeers(Integer.MAX_VALUE)) {
//                if(!lastround.get(Network.get(i).getID()).contains(peer.getID())) {
//                    lastRoundMemory.put(Network.get(i).getID(), lastRoundMemory.get(Network.get(i).getID()) + 1);
//                }
//            }
//            // now rewrite lastround
//            lastround.put(Network.get(i).getID(), observer.nodes.get(Network.get(i).getID()).neighbors);


            if (!observed.containsKey(Network.get(i).getID())) {
                observed.put(Network.get(i).getID(), new LinkedHashSet<>());
                observed.get(Network.get(i).getID()).add(Network.get(i).getID());
            }
            if (observed.get(Network.get(i).getID()).size() == Network.size() && !finished.containsKey(Network.get(i).getID())) {
                finished.put(Network.get(i).getID(), currentTick);
                System.out.println(String.join(",", new String[]{
                        String.valueOf(Network.get(i).getID()),
                        String.valueOf(currentTick),
                        String.valueOf(Network.size()),
                        //String.valueOf(lastRoundMemory.get(Network.get(i).getID())),
                        String.valueOf(withoutMemory.get(Network.get(i).getID()))
                }));
            } else {
                for (Long peer : newPeers) {
                    observed.get(Network.get(i).getID()).add(peer);
                }
            }
        }
//        Stat stat = new Stat(observed, finished);
//        String[] result = {
//                String.valueOf(currentTick),
//                String.valueOf(stat.meanFinished),
//                String.valueOf(stat.meanObserved),
//                String.valueOf(stat.minObserved),
//                String.valueOf(stat.maxObserved)
//        };
        //System.out.println(String.join(",", result));
        if (finished.size() == Network.size()) {
//            System.err.println("Finish: " + finished.size());
//            System.err.println("Observed: " + stat.meanObserved);
//            System.err.println("Mean finished: " + stat.meanFinished);
            exit(0);
        }
    }

    private List<Long> computeRealNewPeers(long id, Long oldest, List<Long> previousPartialview, List<Long> previousSample, List<Long> currentPartialview) {
        // firstly, remove the oldest from the old partialview
        List<Long> oldpv = new ArrayList<>(previousPartialview);
        oldpv.remove(oldest);
        // secondly, remove the id from the old sample
        List<Long> oldsample = new ArrayList<>(previousSample);
        oldsample.remove(id);
        // thridly, substract oldpv wiith oldsample
        oldpv.removeAll(oldsample);
        // then remove all entry in newpv containing remaining oldpv entries
        List<Long> remainings = new ArrayList<>(currentPartialview);
        remainings.removeAll(oldpv);
        return remainings;
    }

    private double meanFinished() {
        double mean = 0;
        for (Long value : finished.values()) {
            mean += value;
        }
        return mean / Network.size();
    }

    private double meanObservedPv() {
        double mean = 0;
        for (Set<Long> value : observed.values()) {
            mean += value.size();
        }
        return mean / Network.size();
    }

    private double minObservedPv() {
        double min = -1;
        for (Set<Long> value : observed.values()) {
            if (min == -1) {
                min = value.size();
            } else if (value.size() < min) {
                min = value.size();
            }
        }
        return min;
    }

    private double maxObservedPv() {
        double max = -1;
        for (Set<Long> value : observed.values()) {
            if (max == -1) {
                max = value.size();
            } else if (value.size() > max) {
                max = value.size();
            }
        }
        return max;
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }

    class Stat {
        public double meanObserved;
        public double meanFinished;
        public double minObserved;
        public double maxObserved;
        Stat(HashMap<Long, Set<Long>> observed, HashMap<Long, Long> finished) {
            meanObserved = 0;
            minObserved = -1;
            for (Set<Long> value : observed.values()) {
                meanObserved += value.size();
                if (minObserved == -1) {
                    minObserved = value.size();
                } else if (value.size() < minObserved) {
                    minObserved = value.size();
                }
                if (maxObserved == -1) {
                    maxObserved = value.size();
                } else if (value.size() > maxObserved) {
                    maxObserved = value.size();
                }
            }
            meanFinished = 0;
            for (Long value : finished.values()) {
                meanFinished += value;
            }
            meanObserved = meanObserved / Network.size();
            meanFinished = meanFinished / Network.size();

        }

    }
}
