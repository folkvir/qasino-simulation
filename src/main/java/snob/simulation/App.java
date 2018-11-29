package snob.simulation;

import peersim.Simulator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("--init")) {
            int peers = 1000;
            int cycles = 10000; // will stop at the end of all queries anyway, but the stop case is around n * log (n)

            int points = 100;
            int[] replicate = new int[points + 1];
            replicate[0] = 0;
            for(int i = 1; i<=points; ++i) {
                replicate[i] = (int) Math.floor(peers / (i));
            }

            int delta_rps = 1;
            int delta_son = 1;
            int rps_size = 5;
            int son_size = 4;
            int qlimit = -1; // unlimited
            int dlimit = -1; // unlimited
            boolean[] son_activated = {false};
            boolean[] trafficMin = {true};
            // firstly do it with only the rps
            for (int i : replicate) {
                for (boolean b : son_activated) {
                    int rpss;
                    if (b) {
                        rpss = rps_size;
                    } else {
                        rpss = 2 * rps_size;
                    }
                    for (boolean traffic : trafficMin) {
                        // create a file
                        // first copy the template
                        System.out.println("Copying template to config...");
                        String configName = "p" + peers
                                + "-son" + b
                                + "-rep" + i
                                + "-traffic" + traffic
                                + "-config.conf";
                        String pathTemplate = System.getProperty("user.dir") + "/configs/template.conf";
                        String pathConfig = System.getProperty("user.dir") + "/configs/generated/" + configName;
                        System.out.println("Template location: " + pathTemplate);
                        System.out.println("Config location: " + pathConfig);
                        File in = new File(pathTemplate);
                        File out = new File(pathConfig);
                        out.createNewFile();
                        copyFileUsingStream(in, out);
                        System.out.println("Replacing config vars to their values...");
                        replace(pathConfig, "\\$son_activated\\$", String.valueOf(b));
                        replace(pathConfig, "\\$traffic\\$", String.valueOf(traffic));
                        replace(pathConfig, "\\$size\\$", String.valueOf(peers));
                        replace(pathConfig, "\\$cycle\\$", String.valueOf(cycles));
                        replace(pathConfig, "\\$rps_size\\$", String.valueOf(rpss));
                        replace(pathConfig, "\\$son_size\\$", String.valueOf(son_size));
                        replace(pathConfig, "\\$rps_delta\\$", String.valueOf(delta_rps));
                        replace(pathConfig, "\\$son_delta\\$", String.valueOf(delta_son));
                        replace(pathConfig, "\\$replicate\\$", String.valueOf(i));
                        replace(pathConfig, "\\$qlimit\\$", String.valueOf(qlimit));
                        replace(pathConfig, "\\$dlimit\\$", String.valueOf(dlimit));
                        replace(pathConfig, "\\\\$", String.valueOf(dlimit));
                        System.out.printf("Executing: peers=%d cycles=%d rps_size=%d son_size=%d rps_delta=%d son_delta=%d replica=%d traffic=%b %n",
                                peers, cycles, rps_size, son_size,
                                delta_rps, delta_rps, i, traffic);
                    }
                }
            }
        } else if (args.length > 0 && args[0].equals("--config")) {
            String[] config = {"./configs/generated/" + args[1], "./results/" + args[1] + "-output.txt"};
            executeConfig(config[0], config[1]);
        }
    }

    private static void replace(String filename, String old, String newOne) throws IOException {
        Path path = Paths.get(filename);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(old, newOne);
        Files.write(path, content.getBytes(charset));
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
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

    protected static PrintStream outputFile(String name) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(name));
    }

    private static void executeConfig(String config, String output) {
        try {
            // Store console print stream.
            PrintStream ps_console = System.out;
            System.setOut(outputFile(output));
            String[] arguments = {config};
            // load the class
            Simulator sim = new Simulator();
            sim.main(arguments);
            // set to console print
            System.setOut(ps_console);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
