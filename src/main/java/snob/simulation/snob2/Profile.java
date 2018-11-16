package snob.simulation.snob2;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.util.ResultSetUtils;
import snob.simulation.snob2.data.InvertibleBloomFilter;

import java.util.*;

public class Profile {
    public int WEIGH_EQUIVALENCE = Integer.MAX_VALUE;
    public int WEIGH_CONTAINMENT = 2;
    public int WEIGH_SUBSET = 1;

    public List<Triple> patterns;
    public Map<Triple, InvertibleBloomFilter> invertibles;
    public int cellCount;
    public int hashCount;
    public QuerySnob query;
    public boolean has_query = false;
    public long qlimit = 1; // number of queries in the network
    // Datastore
    public Datastore datastore;

    public Profile(int ibflCounCell, int ibflHashCount) {
        cellCount = ibflCounCell;
        hashCount = ibflHashCount;
        this.patterns = new ArrayList<>();
        this.invertibles = new HashMap<>();
        this.datastore = new Datastore();
    }

    public void insertTriples(Map<Triple, Iterator<Triple>> its) {
        its.forEach((pattern, iterator) -> {
            List<Triple> list = new ArrayList<>();
            int count = 0;
            // consume the iterator and fill the query
//            System.err.println(" ");
//            System.err.println("Consuming the iterator for the pattern: " + pattern.toString());
            while(iterator.hasNext()) {
                Triple t = iterator.next();
                // populate the query plan
                query.plan.insertTriple(pattern, t);
                // populate the bloom filter associated to the pattern
                invertibles.get(pattern).insert(t);
                list.add(t);
                count++;
                // System.err.print(".");
            }
            // System.err.print("!end! count=" + count);
            // insert triples in datastore
            datastore.insertTriples(list);
        });
    }

    public void update(String query) {
        has_query = true;
        UUID id = UUID.randomUUID();
        this.query = new QuerySnob(query);
        System.err.printf("[update-string] Updating the profile a query expecting %d result(s) %n", this.query.cardinality);
        patterns = this.query.plan.patterns;
        createInvertiblesFromPatterns(patterns);
        initPipeline(patterns);
    }

    private void createInvertiblesFromPatterns(List<Triple> patterns) {
        for (Triple pattern : patterns) {
            if(!this.invertibles.containsKey(pattern)){
                invertibles.put(pattern, new InvertibleBloomFilter(cellCount, hashCount));
            }
        }
    }

    public void update(String query, long card) {
        has_query = true;
        UUID id = UUID.randomUUID();
        this.query = new QuerySnob(query, card);
        System.err.printf("[update-string-card] Updating the profile with a query expecting %d result(s) %n", this.query.cardinality);
        patterns = this.query.plan.patterns;
        createInvertiblesFromPatterns(patterns);
        initPipeline(patterns);
    }

    private void initPipeline(List<Triple> patterns) {
        for (Triple pattern : patterns) {
            this.datastore.getTriplesMatchingTriplePattern(pattern).forEachRemaining(triple -> {
                // populate the query plan
                this.query.plan.insertTriple(pattern, triple);
                // populate the bloom filter associated to the pattern
                invertibles.get(pattern).insert(triple);
            });
        }
    }

    public void execute () {
        try {
            if(this.query != null) {
                ResultSet set = this.query.plan.execute();
                if(this.query.results == null) {
                    this.query.results = set;
                } else {
                    this.query.results = ResultSetUtils.union(this.query.results, set);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Score the provided profile among us
     * high value means that the profile is very interesting
     * @param p the profile to compare with.
     * @return a score based triple pattern containment
     */
    public int score(Profile p) {
        int score = 0;
        boolean stop = false;
        Iterator<Triple> it = p.patterns.iterator();
        while(!stop && it.hasNext()) {
            Triple pt = it.next();
            Iterator<Triple> ittpqs = this.patterns.iterator();
            while(!stop && ittpqs.hasNext()) {
                Triple us = ittpqs.next();
                if (this.equivalence(us, pt)) {
                    stop = true; // we have the highest score, stop or it will cause an overflow
                } else if (this.containment(us, pt)) {
                    try {
                        score = Math.addExact(score, WEIGH_CONTAINMENT);
                    } catch (ArithmeticException e) {
                        stop = true;
                    }
                } else if (this.subset(us, pt)) {
                    try {
                        score = Math.addExact(score, WEIGH_SUBSET);
                    } catch (ArithmeticException e) {
                        stop = true;
                    }
                }
            }
        }
        if(stop) {
            return WEIGH_EQUIVALENCE;
        }
        return score;
    }

    public boolean equivalence(Triple tpa, Triple tpb) {
        return tpa.equals(tpb);
    }
    public boolean containment(Triple tpa, Triple tpb) {
        return this.contain(tpa.getSubject(), tpb.getSubject()) &&
                contain(tpa.getPredicate(), tpb.getPredicate()) &&
                contain(tpa.getObject(), tpb.getObject());
    }
    public boolean subset(Triple tpa, Triple tpb) {
        return this.sub(tpa.getSubject(), tpb.getSubject()) &&
                sub(tpa.getPredicate(), tpb.getPredicate()) &&
                sub(tpa.getObject(), tpb.getObject());
    }
    public boolean contain(Node v1, Node v2) {
       return this.eq(v1, v2) || ( !v1.isVariable() && v2.isVariable());
    }
    public boolean sub(Node v1, Node v2) {
        return this.eq(v1, v2) || ( v1.isVariable() && !v2.isVariable());
    }
    public boolean eq(Node v1, Node v2) {
        return v1.equals(v2);
    }
}

