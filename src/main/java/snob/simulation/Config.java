package snob.simulation;

import peersim.Simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Config implements Runnable {
    public String input;
    public String output;

    public Config(String input, String output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        System.out.printf("Threading %s ...%n", input);
        executeConfig(input, output);
    }

    private void executeConfig(String config, String output) {
        try {
            // Store console print stream.
            PrintStream ps_console = System.out;
            System.setOut(outputFile(output));
            String[] arguments = {config};
            // load the class
            Class.forName(Simulator.class.getName()).getClassLoader().loadClass(Simulator.class.getCanonicalName());
            Simulator.main(arguments);
            // set to console print
            System.setOut(ps_console);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PrintStream outputFile(String name) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(name));
    }
}
