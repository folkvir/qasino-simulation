package snob.simulation.customobservers;

import peersim.core.CommonState;
import peersim.core.Network;
import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;
import snob.simulation.spray.Spray;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.exit;

public class SprayEstimatorObserver implements ObserverProgram {
    // without memory
    private HashMap<Long, Integer> withoutMemory = new HashMap<>();
    // with full memory
    private HashMap<Long, Set<Long>> observed = new HashMap<>();
    private HashMap<Long, Long> finished = new HashMap<>();
    private int peer = CommonState.r.nextInt(Network.size() -1);

    public SprayEstimatorObserver(String prefix) {
    }

    @Override
    public void tick(long currentTick, DictGraph observer) {
        if(currentTick == 50) {
            Spray.start = true;
        }
        if(currentTick > 50.0) {
            double mean = observer.meanPartialViewSize();
            // for (int i = 0; i < Network.size(); ++i) {
            Spray spray = (Spray) observer.nodes.get(Network.get(peer).getID()).pss;
            System.out.println(String.join(",", new String[]{
                    String.valueOf(spray.node.getID()),
                    String.valueOf(currentTick),
                    String.valueOf(spray.estimator),
                    String.valueOf(spray.estimateSize()),
                    String.valueOf(mean)
            }));
            // }
        }
    }



    @Override
    public void onLastTick(DictGraph observer) {
        for (int i = 0; i < Network.size(); ++i) {
            Spray spray = (Spray) observer.nodes.get(Network.get(i).getID()).pss;
                System.out.println(String.join(",", new String[]{
                        String.valueOf(spray.node.getID()),
                        String.valueOf(spray.estimator),
                        String.valueOf(spray.estimateSize()),
                        String.valueOf(observer.meanPartialViewSize()),
                        String.valueOf(Math.exp(observer.meanPartialViewSize())),
                        String.valueOf(Math.log(Network.size())),
                        String.valueOf(Network.size())
                }));
        }
    }
}
