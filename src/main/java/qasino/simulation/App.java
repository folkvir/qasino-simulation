package qasino.simulation;

import peersim.Simulator;
import picocli.CommandLine;

@CommandLine.Command(description = "Run a Qasino experiment using PeerSim, if parameters are set, one experiment is made for each kind of parameters.",
        name = "qasino", mixinStandardHelpOptions = true)
public class App implements Runnable {

    @CommandLine.Option(names = {"--execute"}, description = "Execute the configuration file place in configs/")
    String config = null;

    public static void main(String[] args) {
        CommandLine.run(new App(), args);
    }

    @Override
    public void run() {
        if (config != null) {
            try {
                new Simulator().main(new String[]{config});
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            CommandLine.usage(this, System.out);
        }
    }
}
