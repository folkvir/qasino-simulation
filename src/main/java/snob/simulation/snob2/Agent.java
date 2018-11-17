package snob.simulation.snob2;

import java.lang.instrument.Instrumentation;

public class Agent {
    private static volatile Instrumentation globalInstr;
    public static void premain(String args, Instrumentation inst) {
        System.err.println("Agent loaded.");
        globalInstr = inst;
    }
    public static long getObjectSize(Object obj) {
        if (globalInstr == null)
            throw new IllegalStateException("Agent not initted");
        return globalInstr.getObjectSize(obj);
    }
}
