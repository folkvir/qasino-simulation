package snob.simulation;

import peersim.Simulator;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        String[] config = {"./configs/"+args[0], "./results/"+args[0]+"-output.txt"};
        executeConfig(config[0], config[1]);
    }

    private static void executeConfig(String config, String output) {
        try {
            // Store console print stream.
            PrintStream ps_console = System.out;
            System.setOut(outputFile(output));
            String[] arguments = {config};
            Simulator.main(arguments);
            // set to console print
            System.setOut(ps_console);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static PrintStream outputFile(String name) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(name));
    }
}
