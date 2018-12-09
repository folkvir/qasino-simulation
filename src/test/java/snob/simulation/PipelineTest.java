package snob.simulation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import snob.simulation.snob2.Profile;
import snob.simulation.snob2.data.IBFStrata;
import snob.simulation.snob2.data.Strata.Cell;
import snob.simulation.snob2.data.Strata.IBF;
import snob.simulation.snob2.data.Strata.StrataEstimator;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

public class PipelineTest {
    @Ignore
    @Test
    public void testQ63() {
        Profile p = new Profile();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> p.datastore.update(f.toString()));

        p.update("SELECT DISTINCT  ?x3 ?x4 ?x2 ?x5 WHERE   { <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> ?x2 .  " +
                "   <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?x3 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www.w3.org/2000/01/rdf-schema#label> ?x4 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/class> ?x5 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseaseSubtypeOf> ?x6   } ", 2);

        int count = 0;
        for (Triple pattern : p.query.patterns) {
            int s = p.datastore.getTriplesMatchingTriplePatternAsList(pattern).size();
            count += s;
            System.err.println("tp count: " + s);
        }
        System.err.println("Count: " + count);
    }
    @Ignore
    @Test
    public void testIBF() {
        Profile p = new Profile();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName) -> filenames.add(fileName));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> p.datastore.update(f.toString()));

        int card = 2;
        String q = "SELECT DISTINCT  ?x3 ?x4 ?x2 ?x5 WHERE   { <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> ?x2 .  " +
                "   <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?x3 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www.w3.org/2000/01/rdf-schema#label> ?x4 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/class> ?x5 .     " +
                "<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/2804> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseaseSubtypeOf> ?x6   } ";
        p.update(q, card);

        Profile p1 = new Profile();
        p1.update(q, card);
        int triples = 0;
        // first A ask to B
        for (Triple pattern : p.query.patterns) {
            IBFStrata ibfp = p.query.strata.get(pattern);
            IBFStrata ibfp1 = p1.query.strata.get(pattern);
            // simulate estomator exchange
            StrataEstimator ibfpe1 = ibfp1.getEstimator();
            StrataEstimator ibfe = ibfp.getEstimator();
            int diffsize = ibfe.decode(ibfpe1);
            System.err.println("Diffsize: " + diffsize);
            // get back the ibf of size diffsize, ... skip all vicious part
            Cell[] cells = ibfp.ibf.subtract(ibfp1.ibf.getCells());
            List<Integer>[] diff = ibfp.ibf.decode(cells);
            List<Integer> plus = diff[0];
            Assert.assertEquals(diffsize, plus.size());
            List<Integer> miss = diff[1];
            Assert.assertEquals(0, miss.size());
        }

        // then B ask to A
        for (Triple pattern : p.query.patterns) {
            IBFStrata ibfp = p.query.strata.get(pattern);
            IBFStrata ibfp1 = p1.query.strata.get(pattern);
            // simulate estomator exchange
            StrataEstimator ibfpe1 = ibfp1.getEstimator();
            StrataEstimator ibfe = ibfp.getEstimator();
            int diffsize = ibfpe1.decode(ibfe);
            System.err.println("Diffsize: " + diffsize);
            // get back the ibf of size diffsize, ... skip all vicious part
            Cell[] cells = ibfp1.ibf.subtract(ibfp.ibf.getCells());
            List<Integer>[] diff = ibfp1.ibf.decode(cells);
            List<Integer> plus = diff[0];
            plus.forEach((hash) -> {
                // System.err.println("Plus: " + hash+ " = " + ibfp1.data.get(hash));
            });
            List<Integer> miss = diff[1];
            List<Triple> missingtriples = new LinkedList<>();
            for (Integer integer : miss) {
                triples++;
                missingtriples.add(ibfp.data.get(integer));
                System.err.println("Miss: " + integer + " = " + ibfp.data.get(integer));
            }
            p1.insertTriplesWithList(pattern, missingtriples, true);
            p1.query.strata.get(pattern).insert(missingtriples);

            List<Triple> list = p1.datastore.getTriplesMatchingTriplePatternAsList(pattern);
            Assert.assertEquals(diffsize, list.size());
        }
        Assert.assertEquals(6, triples);

        // first A ask to B
        for (Triple pattern : p.query.patterns) {
            IBFStrata ibfp = p.query.strata.get(pattern);
            IBFStrata ibfp1 = p1.query.strata.get(pattern);
            // simulate estomator exchange
            StrataEstimator ibfpe1 = ibfp1.getEstimator();
            StrataEstimator ibfe = ibfp.getEstimator();
            int diffsize = ibfe.decode(ibfpe1);
            System.err.println("Diffsize: " + diffsize);
            Assert.assertEquals(0, diffsize);
            // get back the ibf of size diffsize, ... skip all vicious part
            Cell[] cells = ibfp.ibf.subtract(ibfp1.ibf.getCells());
            List<Integer>[] diff = ibfp.ibf.decode(cells);
            Assert.assertEquals(0, diffsize);
            Assert.assertEquals(null, diff);
        }
    }

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
