package snob.simulation;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import snob.simulation.snob2.Datastore;
import snob.simulation.snob2.QuerySnob;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;

public class GenerateTest {
    @Ignore
    @Test
    public void GenerateDiseasomeDataset() {
        Datastore d = new Datastore();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> {
            d.update(f.toString());
        });

        // once all fragments loaded
        String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries.json";
        String diseasomeQueryGenerated = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(diseasomeQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.forEach((q) -> {
                JSONObject j = (JSONObject) q;
                QuerySnob query = new QuerySnob(j);
                // execute triple patterns
                Map<String, Integer> patsi = new LinkedHashMap<>();
                int tpnumber = query.patterns.size();
                System.err.println(tpnumber);
                for (Triple pattern : query.patterns) {
                    System.err.println(pattern);
                    patsi.put(pattern.toString(), d.getTriplesMatchingTriplePatternAsList(pattern).size());
                }


                // execute the query
                ResultSet res = d.select(query.realQuery);
                long cpt = 0;
                // write to a ByteArrayOutputStream
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                ResultSetFormatter.outputAsJSON(outputStream, res);
                String json = new String(outputStream.toByteArray());
                JSONObject resultJson = null;
                try {
                    resultJson = (JSONObject) parser.parse(json);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                JSONArray results = (JSONArray) ((JSONObject) resultJson.get("results")).get("bindings");
                j.put("card", results.size());
                j.remove("results");
                j.put("results", resultJson.get("results"));
                j.put("tpsnumber", tpnumber);
                j.put("patterns", patsi);
            });
            File file = new File(diseasomeQueryGenerated);
            file.createNewFile();
            // creates a FileWriter Object
            FileWriter writer = new FileWriter(file);
            writer.write(jsonArray.toString());
            writer.flush();
            writer.close();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
