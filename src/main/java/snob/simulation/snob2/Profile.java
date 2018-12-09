package snob.simulation.snob2;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Profile {
    public int inserted = 0;
    public int WEIGH_EQUIVALENCE = Integer.MAX_VALUE;
    public int WEIGH_CONTAINMENT = 2;
    public int WEIGH_SUBSET = 1;
    public boolean has_query = false;
    public long replicate = 50; // replicate factor in % (one query is replicated over a limited number of peer, 'replicate is this number)

    public QuerySnob query;
    public Datastore datastore = new Datastore();

    public int insertTriplesWithList(Triple pattern, List<Triple> list, boolean traffic) {
        inserted += list.size();
        List<Triple> ibf = new LinkedList<>();
        for (Triple triple : list) {
            if (!datastore.contains(triple)) {
                // System.err.println("Insert in the database and pipeline: " + triple);
                ibf.add(triple);
                query.insertTriple(pattern, triple);
            }
        }
        datastore.insertTriples(ibf);
        return list.size();
    }

    /**
     * Update the profile with a new Query as string only
     *
     * @param query
     */
    public void update(String query) {
        try {
            this.reset();
            has_query = true;
            this.query = new QuerySnob(query);
            // System.err.printf("[update-string] Updating the profile a query expecting %d result(s) %n", this.query.cardinality);
            this.query.patterns = this.query.plan.patterns;
            init(this.query.patterns);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Reset the structure
     */
    private void reset() {
        this.has_query = false;
        this.query = null;
    }

    /**
     * Update the profile with a new Query as string and its number of results
     *
     * @param query
     * @param card
     */
    public void update(String query, long card) {
        try {
            this.reset();
            this.has_query = true;
            this.query = new QuerySnob(query, card);
            // System.err.printf("[update-string-card] Updating the profile with a query expecting %d result(s) %n", this.query.cardinality);
            this.query.patterns = this.query.plan.patterns;
            this.init(this.query.patterns);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Initialize the pipeline of iterators using data stored in the datastore
     * And also initialize Invertible Bloom Filters
     *
     * @param patterns
     */
    private void init(List<Triple> patterns) {
        // System.err.println("[INIT] Initializing the pipeline...");
        for (Triple pattern : patterns) {
            // System.err.printf("[INIT] Inserting triples from %s into the pipeline: ", pattern.toString());
            for (Triple triple : this.datastore.getTriplesMatchingTriplePatternAsList(pattern)) {
                this.query.insertTriple(pattern, triple);
            }
        }
    }

    /**
     * Execute the query using the pipeline of iterators
     */
    public void execute() {
        try {
            if (this.query != null && !this.query.terminated) {
                this.query.execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Score the provided profile among us
     * high value means that the profile is very interesting
     *
     * @param p the profile to compare with.
     * @return a score based triple pattern containment
     */
    public int score(Profile p) {
        int score = 0;
        if (p.query == null) {
            return score;
        } else {
            boolean stop = false;
            Iterator<Triple> it = p.query.patterns.iterator();
            while (!stop && it.hasNext()) {
                Triple pt = it.next();
                Iterator<Triple> ittpqs = this.query.patterns.iterator();
                while (!stop && ittpqs.hasNext()) {
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
            if (stop) {
                return WEIGH_EQUIVALENCE;
            }
            return score;
        }
    }

    private boolean equivalence(Triple tpa, Triple tpb) {
        return tpa.equals(tpb);
    }

    private boolean containment(Triple tpa, Triple tpb) {
        return this.contain(tpa.getSubject(), tpb.getSubject()) &&
                contain(tpa.getPredicate(), tpb.getPredicate()) &&
                contain(tpa.getObject(), tpb.getObject());
    }

    private boolean subset(Triple tpa, Triple tpb) {
        return this.sub(tpa.getSubject(), tpb.getSubject()) &&
                sub(tpa.getPredicate(), tpb.getPredicate()) &&
                sub(tpa.getObject(), tpb.getObject());
    }

    private boolean contain(Node v1, Node v2) {
        return this.eq(v1, v2) || (!v1.isVariable() && v2.isVariable());
    }

    private boolean sub(Node v1, Node v2) {
        return this.eq(v1, v2) || (v1.isVariable() && !v2.isVariable());
    }

    private boolean eq(Node v1, Node v2) {
        return v1.equals(v2);
    }
}

