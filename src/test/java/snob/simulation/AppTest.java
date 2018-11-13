package snob.simulation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FileUtils;
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
import snob.simulation.snob.Datastore;
import snob.simulation.snob.Profile;
import snob.simulation.snob.QuerySnob;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Vector;
import java.util.stream.Stream;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileShouldUpdateWithQuery()
    {
        Profile p = new Profile();
        p.update("PREFIX foaf:  <http://xmlns.com/foaf/0.1/>"+
                "SELECT DISTINCT ?name ?nick {" +
                "   ?x foaf:mbox <mailt:person@server> ."+
                "   ?x foaf:name ?name" +
                "   OPTIONAL { ?x foaf:nick ?nick }" +
                "}");
        Assert.assertEquals(3, p.tpqs.size());
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
        Profile p = new Profile();
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile();
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
        Profile p = new Profile();
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile();
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
        Profile p = new Profile();
        p.update(query);
        // System.out.println(p.tpqs.toString());

        Profile p2 = new Profile();
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
