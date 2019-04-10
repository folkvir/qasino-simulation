package snob.simulation.customobservers;

import peersim.core.Network;
import peersim.core.Node;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.spray.Spray;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SprayMontecarloEstimatorObserver implements ObserverProgram {
    // proportion expected
    double p = 0.99999;

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
            Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;

            double local = estimateLocal(spray);
            double neighbours = estimateLocalPlusNeighbours(spray, observer);
            double localAverage = estimateLocalAverage(spray);
            double neighboursAverage = estimateLocalPlusNeighboursAverage(spray, observer);

            double kmax1 = local *  Math.log(1 / (1 - p));
            double kmax2 = neighbours *  Math.log(1 / (1 - p));
            double kmax3 = localAverage *  Math.log(1 / (1 - p));
            double kmax4 = neighboursAverage *  Math.log(1 / (1 - p));

            String[] toPrint = {
                    String.valueOf(spray.node.getID()),
                    String.valueOf(currentTick),
                    String.valueOf(local),
                    String.valueOf(neighbours),
                    String.valueOf(localAverage),
                    String.valueOf(neighboursAverage),
                    String.valueOf(kmax1),
                    String.valueOf(kmax2),
                    String.valueOf(kmax3),
                    String.valueOf(kmax4),
            };
            System.out.println(String.join(",", toPrint));


        }

        if(currentTick > Network.size() * Math.log(1 / (1 - p))) {
            exit(0);
        }
    }

    @Override
    public void onLastTick(DictGraph observer) {

    }

    public double estimateLocal(Spray node) {
        return Math.exp(node.partialView.size());
    }

    public double estimateLocalPlusNeighbours(Spray node, DictGraph observer) {
        double sum = 0;
        System.out.println(node.partialView.getPeers().size());
        for (Node node1 : node.partialView.getPeers()) {
            Spray spray = (Spray) observer.nodes.get(node1.getID()).pss;
            sum += spray.partialView.size();
            System.out.println(sum + " _ " + spray.partialView.size());
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
        nodeest.sumLocalPlusNeighbours += estimateLocal(node);

        this.estimators.put(node.node.getID(), nodeest);

        return nodeest.sumLocalPlusNeighbours / nodeest.iterationLocalPlusNeighbours;
    }
}
