package snob.simulation.snob2;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

public class Utils {
    public static long getObjectSize(Object o) {
        return ObjectSizeCalculator.getObjectSize(o);
    }
    public static long getObjectSizeByAgent(Object o) {
        return Agent.getObjectSize(o);
    }
}
