package snob.simulation;

import com.google.common.hash.HashCode;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.junit.Assert;
import org.junit.Test;
import snob.simulation.snob2.Profile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.hash.Hashing.crc32;
import static com.google.common.hash.Hashing.murmur3_128;

public class IbflTest {
    @Test
    public void testHashMurmur() {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = murmur3_128().hashBytes(xs.getBytes());
        HashCode xse2 = murmur3_128().hashBytes(xs.getBytes());
        System.out.printf("Hash Murmur Length: %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }

    @Test
    public void testChecksum() {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = crc32().hashBytes(xs.getBytes());
        HashCode xse2 = crc32().hashBytes(xs.getBytes());
        System.out.printf("Checksum Length : %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }

    /**
     * //     * Update fonction of profile should extract tpq
     * //
     */
    @Test
    public void test2peersStratEstimator() {
        System.err.println("HEY COUCOU");
        String query = "PREFIX ns: <http://example.org/ns#> \n" +
                "PREFIX : <http://example.org/ns#> \n" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?z . }";
        // init peer 1
        int count = 0;
        int dtriples = 0;
        Profile p1 = new Profile();
        p1.datastore.update("./datasets/test-peer1.ttl");
        p1.update(query);
        Iterator<Triple> it = p1.datastore.getTriplesMatchingTriplePattern(new Triple(Var.alloc("x"), Var.alloc("y"), Var.alloc("z")));
        while (it.hasNext()) {
            System.err.println("[1] Triple: " + it.next());
            dtriples++;
        }
        Assert.assertEquals(2, dtriples);
        p1.execute();
        System.err.println(p1.query.getResults());
        count = p1.query.getResults().size();
        System.err.println("Size of: " + count);

        // init peer 2
        Profile p2 = new Profile();
        p2.datastore.update("./datasets/test-peer2.ttl");
        p2.update(query);
        p2.execute();

        // simulate an exchange of triples pattern using the exchange method of strata estimator
        Map<Triple, Iterator<Triple>> result = new HashMap<>();
        p1.strata.forEach((pattern, strata) -> {
            List<Triple> triples = strata._exchange(pattern, p2, 0);
            // add new triple from 2 to 1
            result.put(pattern, triples.iterator());
            Assert.assertEquals(2, strata.count);
        });

        // add new triple from 2 to 1
        p1.insertTriples(result, true);

        // now check we have all triples, including ours and missing triples
        Iterator<Triple> it2 = p1.datastore.getTriplesMatchingTriplePattern(new Triple(Var.alloc("x"), Var.alloc("y"), Var.alloc("z")));
        while (it2.hasNext()) {
            System.err.println("[1] Triple: " + it2.next());
            dtriples++;
        }
        Assert.assertEquals(4, dtriples);
        // execute again
        p1.execute();
        // check the number of results, total = 2 "abc" anc "efg"
        System.err.println(p1.query.getResults());
        count = p1.query.getResults().size();
        Assert.assertEquals(2, count);
    }
}
