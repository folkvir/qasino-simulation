package snob.simulation.customobservers;

import peersim.core.Network;
import peersim.core.Node;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.spray.Spray;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SprayMontecarloEstimatorObserver implements ObserverProgram {
    // proportion expected
    double p = 0.99999;

    private HashMap<Long, Integer> count = new HashMap<>();
    private HashMap<Long, Set<Long>> observed = new HashMap<>();
    private Set<Long> finished = new LinkedHashSet<>();

    // estimators
    private class Estimator {
        public int iterationLocal = 0;
        public double sumLocal = 0;
        public int iterationLocalPlusNeighbours = 0;
        public double sumLocalPlusNeighbours = 0;
    }
    private HashMap<Long, Estimator> estimators = new LinkedHashMap<>();

    public SprayMontecarloEstimatorObserver(String prefix) {
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        boolean finish = true;
        for (int i = 0; i < Network.size(); ++i) {
            if(!finished.contains(Network.get(i).getID())) {
                Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;

                double local = estimateLocal(spray);
                double neighbours = estimateLocalPlusNeighbours(spray, observer);
                double localAverage = estimateLocalAverage(spray);
                double neighboursAverage = estimateLocalPlusNeighboursAverage(spray, observer);

                double kmax1 = local *  Math.log(1 / (1 - p));
                double kmax2 = neighbours *  Math.log(1 / (1 - p));
                double kmax3 = localAverage *  Math.log(1 / (1 - p));
                double kmax4 = neighboursAverage *  Math.log(1 / (1 - p));

                List<Long> currentPartialview = spray.getPeers(Integer.MAX_VALUE).parallelStream().map(node -> {
                    return observer.nodes.get(node.getID()).id;
                }).collect(Collectors.toList());


                List<Long> newPeers = this.computeRealNewPeers(spray.node.getID(), spray.oldest, spray.previousPartialview, spray.previousSample, currentPartialview);
                if (!observed.containsKey(Network.get(i).getID())) {
                    observed.put(Network.get(i).getID(), new LinkedHashSet<>());
                    observed.get(Network.get(i).getID()).add(Network.get(i).getID());
                }

                if (!count.containsKey(Network.get(i).getID())) {
                    count.put(Network.get(i).getID(), 0);
                }

                count.put(Network.get(i).getID(), count.get(Network.get(i).getID()) + newPeers.size());
                for (Long peer : newPeers) {
                    observed.get(Network.get(i).getID()).add(peer);
                }

                String[] toPrint = {
                        String.valueOf(spray.node.getID()),
                        String.valueOf(currentTick),
                        String.valueOf(observed.get(Network.get(i).getID()).size()),
                        String.valueOf(count.get(Network.get(i).getID())),
                        String.valueOf(local),
                        String.valueOf(neighbours),
                        String.valueOf(localAverage),
                        String.valueOf(neighboursAverage),
                        String.valueOf(Network.size() * Math.log(1 / (1 - p))),
                        String.valueOf(kmax1),
                        String.valueOf(kmax2),
                        String.valueOf(kmax3),
                        String.valueOf(kmax4),
                };


                System.out.println(String.join(",", toPrint));

                if(count.get(Network.get(i).getID()) > (Network.size() * Math.log(1 / (1 - p)))) {
                    finished.add(Network.get(i).getID());
                }
            }

            if(finished.size() == Network.size()) {
                exit(0);
            }
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

    @Override
    public void onLastTick(DictGraph observer) {

    }

    public double estimateLocal(Spray node) {
        return Math.exp(node.partialView.size());
    }

    public double estimateLocalPlusNeighbours(Spray node, DictGraph observer) {
        double sum = 0;
        for (Node node1 : node.partialView.getPeers()) {
            Spray spray = (Spray) observer.nodes.get(node1.getID()).pss;
            sum += spray.partialView.size();
        }
        return Math.exp((node.partialView.size() + sum) / (node.partialView.size() + 1));
    }

    public double estimateLocalAverage(Spray node) {
        if(!this.estimators.containsKey(node.node.getID())) this.estimators.put(node.node.getID(), new Estimator());

        Estimator nodeest = this.estimators.get(node.node.getID());
        nodeest.iterationLocal++;
        nodeest.sumLocal += estimateLocal(node);

        this.estimators.put(node.node.getID(), nodeest);

        return nodeest.sumLocal / nodeest.iterationLocal;
    }

    public double estimateLocalPlusNeighboursAverage(Spray node, DictGraph observer) {
        if(!this.estimators.containsKey(node.node.getID())) this.estimators.put(node.node.getID(), new Estimator());

        Estimator nodeest = this.estimators.get(node.node.getID());
        nodeest.iterationLocalPlusNeighbours++;
        nodeest.sumLocalPlusNeighbours += estimateLocalPlusNeighbours(node, observer);

        this.estimators.put(node.node.getID(), nodeest);

        return nodeest.sumLocalPlusNeighbours / nodeest.iterationLocalPlusNeighbours;
    }
}
