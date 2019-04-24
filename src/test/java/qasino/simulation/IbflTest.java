package qasino.simulation;

import com.google.common.hash.HashCode;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import qasino.simulation.qasino.Profile;

import static com.google.common.hash.Hashing.crc32;
import static com.google.common.hash.Hashing.murmur3_32;

public class IbflTest {
    @Ignore
    @Test
    public void testHashMurmur() {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = murmur3_32().hashBytes(xs.getBytes());
        HashCode xse2 = murmur3_32().hashBytes(xs.getBytes());
        System.err.printf("Hash Murmur Length: %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }

    @Ignore
    @Test
    public void testChecksum() {
        String xs = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        String xs2 = "\"<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/212> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/associatedGene> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/genes/PPT1>.";
        HashCode xse = crc32().hashBytes(xs.getBytes());
        HashCode xse2 = crc32().hashBytes(xs.getBytes());
        System.err.printf("Checksum Length : %d vs %d, string (%s vs %s) %n", xse.bits(), xse2.bits(), xse.toString(), xse2.toString());
        Assert.assertEquals(xse, xse2);
    }

    @Ignore
    @Test
    public void test3peersTrafficNormal() {
        String query = "PREFIX ns: <http://example.org/ns#> \n" +
                "PREFIX : <http://example.org/ns#> \n" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?z . }";
        // init peer 1
        int dtriples = 0;
        Profile p1 = new Profile();
        p1.local_datastore.update("./datasets/test-peer1.ttl");
        p1.update(query, 3);
        // init peer 2
        Profile p2 = new Profile();
        p2.local_datastore.update("./datasets/test-peer2.ttl");
        p2.update(query, 1);
        p2.execute();
        // init peer 3
        Profile p3 = new Profile();
        p3.local_datastore.update("./datasets/test-peer3.ttl");
        p3.update(query, 1);
        p3.execute();

        // p2 exchange with p3 first
        for (Triple pattern : p2.query.patterns) {
            p2.insertTriplesWithList(pattern, p3.local_datastore.getTriplesMatchingTriplePatternAsList(pattern));
        }

        p2.local_datastore.getTriplesMatchingTriplePattern(new Triple(Var.alloc("x"), Var.alloc("y"), Var.alloc("z"))).forEachRemaining(triple -> {
            System.err.println("Triple in p2: " + triple);
        });

        // then p1 exchange with p2
        for (Triple pattern : p1.query.patterns) {
            p1.insertTriplesWithList(pattern, p2.local_datastore.getTriplesMatchingTriplePatternAsList(pattern));
        }

        p1.local_datastore.getTriplesMatchingTriplePattern(new Triple(Var.alloc("x"), Var.alloc("y"), Var.alloc("z"))).forEachRemaining(triple -> {
            System.err.println("Triple in p1: " + triple);
        });

        p1.execute();
        Assert.assertEquals(3, p1.query.getResults().size());
    }

    @Ignore
    @Test
    public void test1peer() {
        String query = "PREFIX ns: <http://example.org/ns#> \n" +
                "PREFIX : <http://example.org/ns#> \n" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?z . }";
        // init peer 1
        Profile p1 = new Profile();
        p1.local_datastore.update("./datasets/test-peer1.ttl");
        p1.local_datastore.update("./datasets/test-peer2.ttl");
        p1.update(query, 2);
        p1.execute();
        Assert.assertEquals(2, p1.query.getResults().size());
    }
}
