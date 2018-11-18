package snob.simulation;

import com.google.common.hash.HashCode;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.Assert;
import org.junit.Test;
import snob.simulation.snob2.Profile;
import snob.simulation.snob2.data.InvertibleBloomFilter;

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

    @Test
    public void testReconciliation() {
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
        } catch (Exception e) {
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
     * //     * Update fonction of profile should extract tpq
     * //
     */
    @Test
    public void test2peers() {

        String query = "PREFIX ns: <http://example.org/ns#> \n" +
                "PREFIX : <http://example.org/ns#> \n" +
                "SELECT * WHERE { ?x ns:p ?y . ?y ns:p ?z . }";
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
        System.err.println("[1] Simulate the 1st exchange from P1 to P2");
        Map<Triple, Iterator<Triple>> result = new HashMap<>();
        p1.invertibles.forEach((pattern, ibf) -> {
            ibf.getMapping().forEach((k, v) -> System.err.println("[1] IBF triple:" + v.getTriple().toString()));
            System.err.println("[1] Pattern: " + pattern.toString());
            int count = 0;
            Iterator<Triple> it = p1.datastore.getTriplesMatchingTriplePattern(pattern);
            while(it.hasNext()) {
                Triple t = it.next();
                System.err.println("[1] Reading triple: " + t.toString());
                count++;
            }
            Assert.assertEquals(2, count);
            // for each ibf ask for missing triples to profile 2
            if (p2.invertibles.containsKey(pattern)) {
                List<Triple> absent = p2.invertibles.get(pattern).absentTriple(ibf);
                System.err.println("[1] Missing triples size: " + absent.size());
                Assert.assertEquals(2, absent.size());
                result.put(pattern, absent.iterator());
            } else {
                Iterator<Triple> listTriples = p2.datastore.getTriplesMatchingTriplePattern(pattern);
                InvertibleBloomFilter local = InvertibleBloomFilter.createIBFFromTriples(listTriples, 100, 2);
                List<Triple> absent = local.absentTriple(ibf);
                System.err.println("[1] Missing triples size: " + absent.size());
                Assert.assertEquals(2, absent.size());
                result.put(pattern, absent.iterator());
            }
        });
        // add new triple from 2 to 1
        p1.insertTriples(result);
        p1.execute();
        p1.query.results.forEachRemaining(binding -> {
            System.err.println("[1] first exec: " + binding.toString());
        });

        // simulate an exchange from 1 to 2
        System.err.println("[2] Simulate the 2nd exchange from P1 to P2");
        Map<Triple, Iterator<Triple>> result2 = new HashMap<>();
        p1.invertibles.forEach((pattern, ibf) -> {
            ibf.getMapping().forEach((k, v) -> System.err.println("[2] IBF triple:" + v.getTriple().toString()));
            System.err.println("[2] Triple pattern " + pattern.toString());
            int count = 0;
            Iterator<Triple> it = p1.datastore.getTriplesMatchingTriplePattern(pattern);
            while(it.hasNext()) {
                Triple t = it.next();
                System.err.println("[2] Reading triple: " + t.toString());
                count++;
            }
            Assert.assertEquals(4, count);
            // for each ibf ask for missing triples to profile 2
            if (p2.invertibles.containsKey(pattern)) {
                List<Triple> absent = p2.invertibles.get(pattern).absentTriple(ibf);
                System.err.println("[2] Missing triples size: " + absent.size());
                Object[] clone = absent.toArray().clone();
                for (int i = 0; i < clone.length; i++) {
                    System.err.println("Missing triple: " + clone[i].toString());
                }
                Assert.assertEquals(0, absent.size());

                result2.put(pattern, absent.iterator());
            } else {
                Iterator<Triple> listTriples = p2.datastore.getTriplesMatchingTriplePattern(pattern);
                InvertibleBloomFilter local = InvertibleBloomFilter.createIBFFromTriples(listTriples, 100, 2);
                List<Triple> absent = local.absentTriple(ibf);
                System.err.println("[2] Missing triples size: " + absent.size());
                Assert.assertEquals(0, absent.size());
                result2.put(pattern, absent.iterator());
            }
        });

        p1.insertTriples(result2);
        p1.execute();
        p1.query.results.forEachRemaining(binding -> {
            System.err.println("[2] second exec: " + binding.toString());
        });
        // now p1 should have all results from p2
    }
}
