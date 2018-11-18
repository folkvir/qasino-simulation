package snob.simulation;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.junit.Assert;
import org.junit.Test;
import snob.simulation.snob2.Datastore;

import java.util.Iterator;

public class ProfileTest {
    /**
     * Update fonction of profile should extract tpq
     */
    @Test
    public void profileShouldUpdateWithQuery()
    {
        snob.simulation.snob2.Profile p = new snob.simulation.snob2.Profile(100, 2);
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
        snob.simulation.snob2.Profile p = new snob.simulation.snob2.Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        snob.simulation.snob2.Profile p2 = new snob.simulation.snob2.Profile(100, 2);
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
        snob.simulation.snob2.Profile p = new snob.simulation.snob2.Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        snob.simulation.snob2.Profile p2 = new snob.simulation.snob2.Profile(100, 2);
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
        snob.simulation.snob2.Profile p = new snob.simulation.snob2.Profile(100, 2);
        p.update(query);
        // System.out.println(p.tpqs.toString());

        snob.simulation.snob2.Profile p2 = new snob.simulation.snob2.Profile(100, 2);
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
        snob.simulation.snob2.Datastore d = new Datastore();
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
        snob.simulation.snob2.Profile p = new snob.simulation.snob2.Profile(100, 2);
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
}
