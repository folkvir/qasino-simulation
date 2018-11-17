package snob.simulation;

import com.google.common.hash.HashCode;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.core.Var;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import snob.simulation.snob2.Datastore;
import snob.simulation.snob2.Profile;
import snob.simulation.snob2.QuerySnob;
import snob.simulation.snob2.data.InvertibleBloomFilter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.hash.Hashing.crc32;
import static com.google.common.hash.Hashing.murmur3_128;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void testHashMurmur () {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = murmur3_128().hashBytes(xs.getBytes());
        HashCode xse2 = murmur3_128().hashBytes(xs.getBytes());
        System.out.printf("Hash Murmur Length: %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }

    @Test
    public void testChecksum () {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = crc32().hashBytes(xs.getBytes());
        HashCode xse2 = crc32().hashBytes(xs.getBytes());
        System.out.printf("Checksum Length : %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }
    @Test
    public void testReconciliation () {
        Triple t = new Triple(NodeFactory.createURI("a"),
                NodeFactory.createURI("a"),
                NodeFactory.createURI("a"));
        Triple t2 = new Triple(NodeFactory.createURI("b"),
                NodeFactory.createURI("b"),
                NodeFactory.createURI("b"));
        Triple t3 = new Triple(NodeFactory.createURI("d"),
                NodeFactory.createURI("d"),
                NodeFactory.createURI("d"));
        InvertibleBloomFilter b1 = new InvertibleBloomFilter(10, 2);
        try {
            b1.insert(t);
            b1.insert(t2);
            b1.insert(t3);
        } catch(Exception e) {
            e.printStackTrace();
        }


        InvertibleBloomFilter b2 = new InvertibleBloomFilter(100, 2);
        Triple t4 = new Triple(NodeFactory.createURI("a"),
                NodeFactory.createURI("a"),
                NodeFactory.createURI("a"));
        Triple t5 = new Triple(NodeFactory.createURI("c"),
                NodeFactory.createURI("c"),
                NodeFactory.createURI("c"));
        b2.insert(t4);
        b2.insert(t5);

        // get absent triple, triples that are in A but not in B
        List<Triple> absentTriples = b2.absentTriple(b1);
        absentTriples.forEach(triple -> {
            System.out.println("Absent triple: " + triple.toString());
            Assert.assertEquals(triple.toString(), "c @c c");
        });
    }
    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileShouldUpdateWithQuery()
    {
        Profile p = new Profile(100, 2);
        p.update("PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "   ?x foaf:mbox <mailt:person@server> ."+
                "   ?x foaf:name ?name" +
                "   OPTIONAL { ?x foaf:nick ?nick }" +
                "}");
        Assert.assertEquals(3, p.patterns.size());
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileScoringShouldReturnMaxValue()
    {
        String query = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "   ?x foaf:mbox <mailt:person@server> ."+
                "   ?x foaf:name ?name" +
                "   OPTIONAL { ?x foaf:nick ?nick }" +
                "}";
        Profile p = new Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile(100, 2);
        p2.update(query);
        // System.out.println(p2.tpqs.toString());

        Assert.assertEquals(Integer.MAX_VALUE, p.score(p2));
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileScoringShouldReturn2()
    {
        String query = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "   ?x foaf:mbox <mailt:person@server> ."+
                "   ?x foaf:name ?name" +
                "   OPTIONAL { ?x foaf:nick ?nick }" +
                "}";
        String query2 = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "  ?x ?p <mailt:person@server>"+
                "}";
        Profile p = new Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile(100, 2);
        p2.update(query2);
        // System.out.println(p2.tpqs.toString());

        Assert.assertEquals(2, p.score(p2));
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileScoringShouldReturn3()
    {
        String query = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "   ?x foaf:mbox <mailt:person@server> ."+
                "   ?x foaf:name ?name" +
                "   OPTIONAL { ?x foaf:nick ?nick }" +
                "}";
        String query2 = "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "  ?x ?p <mailt:person@server> ."+
                "  ?x foaf:name \"toto\" "+
                "}";
        Profile p = new Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile(100, 2);
        p2.update(query2);
        // System.out.println(p2.tpqs.toString());

        Assert.assertEquals(3, p.score(p2));
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void DatastoreShouldBeQueryiable()
    {
        Datastore d = new Datastore();
        d.update("./datasets/test.ttl");
        Triple t = new Triple(Var.alloc("s"),
                Var.alloc("p"),
                Var.alloc("o"));
        // System.out.println("Creating the triple pattern: "+t.toString());

        Iterator<Triple> it = d.getTriplesMatchingTriplePattern(t);
        int count = 0;
        while(it.hasNext()) {
            it.next();
            count++;
        }
        Assert.assertEquals(3, count);
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void PipelineIteratorShouldWork()
    {
        Profile p = new Profile(100, 2);
        p.datastore.update("./datasets/test.ttl");
        String query = "SELECT * WHERE { ?s ?p ?o . }";
        p.update(query);
        p.execute();
        ResultSet res = p.query.results;
        int count = 0;
        while(res.hasNext()) {
            count++;
            res.next();
        }
        Assert.assertEquals(3, count); // 3 triples
        Assert.assertEquals(p.invertibles.size(), 1); // one spo
        Triple spo = new Triple(Var.alloc("s"),
                Var.alloc("p"),
                Var.alloc("o"));
        Assert.assertEquals(p.invertibles.get(spo).mydata().size(), 3);
    }

    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void test2peers()
    {

        String query = "PREFIX ns: <http://example.org/ns#>" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?x . }";
        // init peer 1
        Profile p1 = new Profile(100, 2);
        p1.datastore.update("./datasets/test-peer1.ttl");
        p1.update(query);
        p1.execute();
        // init peer 2
        Profile p2 = new Profile(100, 2);
        p2.datastore.update("./datasets/test-peer2.ttl");
        p2.update(query);
        p2.execute();

        // simulate an exchange from 1 to 2
        System.out.println("Simulate an exchange from P1 to P2");
        Map<Triple, Iterator<Triple>> result = new HashMap<>();
        p1.invertibles.forEach((pattern, ibf) -> {
            // for each ibf ask for missing triples to profile 2
            if(p2.invertibles.containsKey(pattern)) {
                List<Triple> absent = p2.invertibles.get(pattern).absentTriple(ibf);
                System.out.println("Missing triples size: " + absent.size());
                Assert.assertEquals(2, absent.size());
                result.put(pattern, absent.iterator());
            } else {
                Iterator<Triple> listTriples = p2.datastore.getTriplesMatchingTriplePattern(pattern);
                InvertibleBloomFilter local = InvertibleBloomFilter.createIBFFromTriples(listTriples, 100, 2);
                List<Triple> absent = local.absentTriple(ibf);
                System.out.println("Missing triples size: " + absent.size());
                Assert.assertEquals(2, absent.size());
                result.put(pattern, absent.iterator());
            }
        });
        // add new triple from 2 to 1
        p1.insertTriples(result);
        p1.execute();
        p1.query.results.forEachRemaining(binding -> {
            System.out.println("first exec: " + binding.toString());
        });

        // simulate an exchange from 1 to 2
        System.out.println("Simulate an exchange from P1 to P2");
        Map<Triple, Iterator<Triple>> result2 = new HashMap<>();
        p1.invertibles.forEach((pattern, ibf) -> {
            // for each ibf ask for missing triples to profile 2
            if(p2.invertibles.containsKey(pattern)) {
                List<Triple> absent = p2.invertibles.get(pattern).absentTriple(ibf);
                System.out.println("Missing triples size: " + absent.size());
                Assert.assertEquals(0, absent.size());
                result2.put(pattern, absent.iterator());
            } else {
                Iterator<Triple> listTriples = p2.datastore.getTriplesMatchingTriplePattern(pattern);
                InvertibleBloomFilter local = InvertibleBloomFilter.createIBFFromTriples(listTriples, 100, 2);
                List<Triple> absent = local.absentTriple(ibf);
                System.out.println("Missing triples size: " + absent.size());
                Assert.assertEquals(0, absent.size());
                result2.put(pattern, absent.iterator());
            }
        });

        p1.insertTriples(result2);
        p1.execute();
        p1.query.results.forEachRemaining(binding -> {
            System.out.println("second exec: " + binding.toString());
        });
        // now p1 should have all results from p2
    }

    @Test
    public void testTwitterObjectSize () {
        String query = "PREFIX ns: <http://example.org/ns#>" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?x . }";
        Profile p = new Profile(100, 2);
        p.datastore.update("./datasets/test-peer1.ttl");
        p.update(query);
    }

    @Test
    public void testPipelineAgainstJenaOverDiseasomeDataset () {
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

    @Test
    public void GenerateDiseasomeDataset() {
        Datastore d = new Datastore();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/diseasome/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName)->filenames.add(fileName));
        } catch(IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> {d.update(f.toString());});

        // once all fragments loaded
        String diseasomeQuery = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries.json";
        String diseasomeQueryGenerated = System.getProperty("user.dir") + "/datasets/data/diseasome/queries/queries_jena_generated.json";
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(diseasomeQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.forEach((q) -> {
                JSONObject j = (JSONObject) q;
                QuerySnob query = new QuerySnob(j);
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
            });
            File file =  new File(diseasomeQueryGenerated);
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

    @Test
    public void GenerateLinkedmdbDataset() {
        Datastore d = new Datastore();
        String diseasome = System.getProperty("user.dir") + "/datasets/data/linkedmdb/fragments/";
        Vector filenames = new Vector();
        try (Stream<Path> paths = Files.walk(Paths.get(diseasome))) {
            paths.filter(Files::isRegularFile).forEach((fileName)->filenames.add(fileName));
        } catch(IOException e) {
            System.err.println(e.toString());
        }
        filenames.forEach(f -> {d.update(f.toString());});

        // once all fragments loaded
        String linkedmdbQuery = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries.json";
        String linkedmdbQueryGenerated = System.getProperty("user.dir") + "/datasets/data/linkedmdb/queries/queries_jena_generated.json";
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(linkedmdbQuery)) {
            JSONArray jsonArray = (JSONArray) parser.parse(is);
            jsonArray.forEach((q) -> {
                JSONObject j = (JSONObject) q;
                QuerySnob query = new QuerySnob(j);
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
            });
            File file =  new File(linkedmdbQueryGenerated);
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
