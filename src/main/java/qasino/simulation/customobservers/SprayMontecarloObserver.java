package qasino.simulation.customobservers;

import peersim.core.Network;
import peersim.core.Node;
import qasino.simulation.observers.DictGraph;
import qasino.simulation.observers.ObserverProgram;
import qasino.simulation.spray.Spray;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SprayMontecarloObserver implements ObserverProgram {
    // proportion expected
    double p = 0.99999;

    private HashMap<Long, Integer> count = new HashMap<>();
    private HashMap<Long, Set<Long>> observed = new HashMap<>();
    private Set<Long> finished = new LinkedHashSet<>();
    private HashMap<Long, Estimator> estimators = new LinkedHashMap<>();

    public SprayMontecarloObserver(String prefix) {
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        boolean finish = true;
        for (int i = 0; i < Network.size(); ++i) {
            if (!finished.contains(Network.get(i).getID())) {
                Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;

                double local = estimateLocal(spray);
                double neighbours = estimateLocalPlusNeighbours(spray, observer);
                double localAverage = estimateLocalAverage(spray);
                double neighboursAverage = estimateLocalPlusNeighboursAverage(spray, observer);

                double kmax1 = local * Math.log(1 / (1 - p));
                double kmax2 = neighbours * Math.log(1 / (1 - p));
                double kmax3 = localAverage * Math.log(1 / (1 - p));
                double kmax4 = neighboursAverage * Math.log(1 / (1 - p));

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

                boolean probafinished = false;
                if (probabilisticIsFinished(p, observed.get(Network.get(i).getID()).size(), count.get(Network.get(i).getID()))) {
                    probafinished = true;
                    // System.err.println("count: " + count.get(Network.get(i).getID()) +  " - montecarlo: " + Network.get(i).getID());
                }
                boolean montecarlofinished = false;
                if (count.get(Network.get(i).getID()) > (Network.size() * Math.log(1 / (1 - p)))) {
                    montecarlofinished = true;
                    // System.err.println("count: " + count.get(Network.get(i).getID()) +  " - proba: " + Network.get(i).getID());
                }

                String[] toPrint = {
                        String.valueOf(spray.node.getID()),
                        String.valueOf(currentTick),
                        String.valueOf(observed.get(Network.get(i).getID()).size()),
                        String.valueOf(count.get(Network.get(i).getID())),
                        String.valueOf(montecarlofinished),
                        String.valueOf(probafinished)
                };
                if (montecarlofinished) {
                    finished.add(Network.get(i).getID());
                }
                System.out.println(String.join(",", toPrint));
            }

            if (finished.size() == Network.size()) {
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
        if (!this.estimators.containsKey(node.node.getID())) this.estimators.put(node.node.getID(), new Estimator());

        Estimator nodeest = this.estimators.get(node.node.getID());
        nodeest.iterationLocal++;
        nodeest.sumLocal += estimateLocal(node);

        this.estimators.put(node.node.getID(), nodeest);

        return nodeest.sumLocal / nodeest.iterationLocal;
    }

    public double estimateLocalPlusNeighboursAverage(Spray node, DictGraph observer) {
        if (!this.estimators.containsKey(node.node.getID())) this.estimators.put(node.node.getID(), new Estimator());

        Estimator nodeest = this.estimators.get(node.node.getID());
        nodeest.iterationLocalPlusNeighbours++;
        nodeest.sumLocalPlusNeighbours += estimateLocalPlusNeighbours(node, observer);

        this.estimators.put(node.node.getID(), nodeest);

        return nodeest.sumLocalPlusNeighbours / nodeest.iterationLocalPlusNeighbours;
    }

    public boolean probabilisticIsFinished(double apriori, double distinct, int numberofpeersseen) {
        if (distinct == 0) return false;
        double infinity = 20;
        double sum = 0;
        for (int i = 0; i < infinity; ++i) {
            double tmp = distinct / (distinct + i);
            double pow = Math.pow(tmp, numberofpeersseen);
            // System.err.println(tmp + " : " + pow);
            sum += pow;
        }
        return (apriori * sum) < 1;
    }

    // estimators
    private class Estimator {
        public int iterationLocal = 0;
        public double sumLocal = 0;
        public int iterationLocalPlusNeighbours = 0;
        public double sumLocalPlusNeighbours = 0;
    }
}
