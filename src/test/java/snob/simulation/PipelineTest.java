package snob.simulation;

import org.apache.jena.query.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import snob.simulation.snob2.Profile;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.stream.Stream;

public class PipelineTest {
    @Test
    @Ignore
    public void testPipelineAgainstJenaOverDiseasomeDataset () throws ParseException {
        Profile p = new Profile(100, 2);
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName)->filenames.add(fileName));
        } catch(IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> p.datastore.update(f.toString()));
        // once all fragments loaded
        String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries.json";
        String diseasomeQueryGenerated = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(diseasomeQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject j = (JSONObject) jsonArray.get(i);
                String query = (String) j.get("query");
                System.out.println(query);
                p.update(query);
                // execute the pipeline over JENA
                ResultSet resJena = p.datastore.select(p.query.realQuery);
                int countJena = 0;
                while(resJena.hasNext()){
                    resJena.next();
                    countJena++;
                }
                System.out.printf("[Q-%d] JENA result has %d results. %n", i, countJena);
                // execute the pipeline over the pipeline
                p.execute();
                ResultSet resPipeline = p.query.results;
                int countPipeline = 0;
                while(resPipeline.hasNext()){
                    resPipeline.next();
                    countPipeline++;
                }
                System.out.printf("[Q-%d] Pipeline result has %d results. %n", i, countPipeline);
                Assert.assertEquals(countPipeline, countJena);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
