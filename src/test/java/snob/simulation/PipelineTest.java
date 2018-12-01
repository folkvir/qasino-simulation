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
    @Ignore
    @Test
    public void ordering() {
        Profile p = new Profile();
        p.datastore.update("./datasets/test-peer1.ttl");
        p.update("PREFIX ns: <http://example.org/ns#> \n" +
                "PREFIX :     <http://example.org/ns#> \n" +
                "SELECT * WHERE   {\n" +
                "  :a ?x :c .\n" +
                // "  ?x :b :c .\n" +
                "  :a :b ?x .\n" +
                "}");
        p.execute();
        p.query.getResults().forEach(sol -> {
            System.err.println(sol);
        });
    }

    @Ignore
    @Test
    public void testPipelineAgainstJenaOverDiseasomeDataset() throws ParseException {
        Profile p = new Profile();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
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
                long card = (long) j.get("card");
                System.err.println(query);
                p.update(query, card);
                // execute the pipeline over JENA
                ResultSet resJena = p.datastore.select(p.query.realQuery);
                int countJena = 0;
                while (resJena.hasNext()) {
                    resJena.next();
                    countJena++;
                }
                System.err.printf("[Q-%d] JENA result has %d results. %n", i, countJena);
                // execute the pipeline over the pipeline
                p.execute();
                System.err.printf("[Q-%d] Pipeline result has %d results. %n", i, p.query.getResults().size());
                Assert.assertEquals(p.query.getResults().size(), countJena);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testPipelineAgainstJenaOverLinkedmdbDataset() throws ParseException {
        Profile p = new Profile();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/linkedmdb/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> p.datastore.update(f.toString()));
        // once all fragments loaded
        String linkedmdbQuery = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries.json";
        String linkedmdbQueryGenerated = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries_jena_generated.json";
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(linkedmdbQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject j = (JSONObject) jsonArray.get(i);
                String query = (String) j.get("query");
                long card = (long) j.get("card");
                System.err.println(query);
                p.update(query, card);
                // execute the pipeline over JENA
                ResultSet resJena = p.datastore.select(p.query.realQuery);
                int countJena = 0;
                while (resJena.hasNext()) {
                    resJena.next();
                    countJena++;
                }
                System.err.printf("[Q-%d] JENA result has %d results. %n", i, countJena);
                // execute the pipeline over the pipeline
                p.execute();
                System.err.printf("[Q-%d] Pipeline result has %d results. %n", i, p.query.getResults().size());
                Assert.assertEquals(p.query.getResults().size(), countJena);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
