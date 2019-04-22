package qasino.simulation;

import peersim.Simulator;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(description = "Run a cCool experiment using PeerSim, if parameters are set, one experiment is made for each kind of parameters.",
        name = "ccool", mixinStandardHelpOptions = true)
public class App implements Runnable {
    @CommandLine.Option(names = "--template", description = "Define the template you want, inside configs folder")
    String template = "template.conf";

    @CommandLine.Option(names = {"--peers"}, description = "Number of peers")
    int peers = 1000;

    @CommandLine.Option(names = {"--cycles"}, description = "Number of cycles")
    int cycles = 100000;

    @CommandLine.Option(names = {"--queries"}, description = "Queries to execute, \"17 22 54 73 87\"")
    String queries2execute = "17 22 54 73 87";

    @CommandLine.Option(names = {"--replicate"}, description = "Number of Replicated queries with the form of \"1 2 3 4 5\" for 1 rep query then 2, then 3 etc...")
    String replicatedQueries = "1 2 4 8 16 32 64 128 256";

    @CommandLine.Option(names = {"--execute"}, description = "Execute the configuration file place in configs/generated")
    String config = null;

    public static void main(String[] args) {
        CommandLine.run(new App(), args);
    }

    private void replace(String filename, String old, String newOne) throws IOException {
        Path path = Paths.get(filename);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(old, newOne);
        Files.write(path, content.getBytes(charset));
    }

    private void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            is.close();
            os.close();
        }
    }

    protected PrintStream outputFile(String name) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(name));
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
            // int cycles = 10000; // will stop at the end of all queries anyway, but the stop case is around n * log (n)

            String[] repq = replicatedQueries.split(" ");
            int[] replicate = new int[repq.length];
            for (int i = 0; i < replicate.length; i++) {
                replicate[i] = Integer.valueOf(repq[i]);
            }
            // int[] replicate = {1, 2, 4, 8, 16, 32, 64, 128, 256};

            String[] q2execute = queries2execute.split(" ");
            int[] queries = new int[q2execute.length];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = Integer.valueOf(q2execute[i]);
            }

            int delta_rps = 1;
            int rps_size = 100;
            int rps_size_exchange = 50;
            int pick = 10;

            boolean[] son_activated = {false};
            boolean[] trafficMin = {true};
            // firstly do it with only the rps
            for (int query : queries) {
                for (int rep : replicate) {
                    for (boolean b : son_activated) {
                        for (boolean traffic : trafficMin) {
                            // create a file
                            // first copy the template
                            System.err.println("Copying template to config...");
                            String configName = "p" + peers
                                    + "-q" + query
                                    + "-son" + b
                                    + "-rep" + rep
                                    + "-traffic" + traffic
                                    + "-config.conf";
                            String pathTemplate = System.getProperty("user.dir") + "/configs/" + template;
                            String pathConfig = System.getProperty("user.dir") + "/configs/generated/" + configName;
                            //System.err.println("Template location: " + pathTemplate);
                            System.err.println("Config location: " + pathConfig);
                            File in = new File(pathTemplate);
                            File out = new File(pathConfig);
                            try {
                                out.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                copyFileUsingStream(in, out);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.err.println("Replacing config vars to their values...");
                            try {
                                replace(pathConfig, "\\$son_activated\\$", String.valueOf(b));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$traffic\\$", String.valueOf(traffic));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$size\\$", String.valueOf(peers));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$cycle\\$", String.valueOf(cycles));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$rps_size\\$", String.valueOf(rps_size));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$rps_size_exchange\\$", String.valueOf(rps_size_exchange));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$pick\\$", String.valueOf(pick));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$rps_delta\\$", String.valueOf(delta_rps));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$replicate\\$", String.valueOf(rep));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                replace(pathConfig, "\\$querytoreplicate\\$", String.valueOf(query));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
}
