package qasino.simulation.observers.program;

import qasino.simulation.observers.DictGraph;
import qasino.simulation.observers.ObserverProgram;

/**
 * Created by julian on 11/05/15.
 */
public class PartialViewSizeProgram implements ObserverProgram {

    public void tick(long currentTick, DictGraph observer) {
        System.err.println(observer.meanPartialViewSize());
    }

    public void onLastTick(DictGraph observer) {

    }
}
