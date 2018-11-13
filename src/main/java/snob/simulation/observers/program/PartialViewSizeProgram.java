package snob.simulation.observers.program;

import snob.simulation.observers.DictGraph;
import snob.simulation.observers.ObserverProgram;

/**
 * Created by julian on 11/05/15.
 */
public class PartialViewSizeProgram implements ObserverProgram {

    public void tick(long currentTick, DictGraph observer) {
        System.out.println(observer.meanPartialViewSize());
    }

    public void onLastTick(DictGraph observer) {

    }
}
