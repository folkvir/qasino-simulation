package snob.simulation.snob2;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import snob.simulation.snob2.data.IBFStrata;

import java.util.*;

public class Profile {
    public int WEIGH_EQUIVALENCE = Integer.MAX_VALUE;
    public int WEIGH_CONTAINMENT = 2;
    public int WEIGH_SUBSET = 1;
    public boolean has_query = false;
    public long qlimit = 1; // number of queries in the network
    public long replicate = 50; // replicate factor in % (one query is replicated over a limited number of peer, 'replicate is this number)

    public List<Triple> patterns = new ArrayList<>();
    public Map<Triple, IBFStrata> strata = new HashMap<>();
    public QuerySnob query;
    public Datastore datastore = new Datastore();
    public Set<Integer> alreadySeen = new HashSet<>();


    public void addAlreadySeend (int remote) {
        alreadySeen.add(remote);
    }
    public void mergeAlreadySeen (Set<Integer> remote) {
        alreadySeen.addAll(remote);
    }
    /**
     * Insert triples when we receive a list of pattern with triples matching these triple patterns from another peer.
     *
     * @param its Iterator of triples associated to a triple pattern.
     */
    public int insertTriples(Map<Triple, Iterator<Triple>> its, boolean traffic) {
        int count = 0;
        for (Map.Entry<Triple, Iterator<Triple>> entry : its.entrySet()) {
            Triple pattern = entry.getKey();
            Iterator<Triple> iterator = entry.getValue();
            List<Triple> list = new ArrayList<>();
            while (iterator.hasNext()) {
                Triple t = iterator.next();
                if (!this.datastore.contains(t)) {
                    count++;
                    // System.err.printf("Adding triple: [%s] for pattern=[%s]to the datastore %n", t.toString(), pattern.toString());
                    // populate the pipeline plan
                    query.plan.insertTriple(pattern, t);
                    // populate the bloom filter associated to the pattern
                }
            }
            // System.err.print("!end! count=" + count);
            // insert triples in datastore
            if (traffic) this.strata.get(pattern).insert(list);
            datastore.insertTriples(list);

        }
        return count;
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
            System.err.printf("[update-string] Updating the profile a query expecting %d result(s) %n", this.query.cardinality);
            patterns = this.query.plan.patterns;
            init(patterns);
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
        this.patterns = new ArrayList<>();
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
            System.err.printf("[update-string-card] Updating the profile with a query expecting %d result(s) %n", this.query.cardinality);
            this.patterns = this.query.plan.patterns;
            this.init(this.patterns);
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
        System.err.println("[INIT] Initializing the pipeline...");
        for (Triple pattern : patterns) {
            System.err.printf("[INIT] Inserting triples from %s into the pipeline: ", pattern.toString());
            List<Triple> list = new ArrayList<>();
            this.datastore.getTriplesMatchingTriplePattern(pattern).forEachRemaining(triple -> {
                // System.err.printf(".");
                // populate the pipeline plan
                this.query.plan.insertTriple(pattern, triple);
                list.add(triple);
            });
            if (!this.strata.containsKey(pattern)) {
                this.strata.put(pattern, new IBFStrata());
            }
            this.strata.get(pattern).insert(list);
            System.err.println(":end.");
        }
    }

    /**
     * Execute the query using the pipeline of iterators
     */
    public void execute() {
        try {
            if (this.query != null) {
                this.query.insertResults(this.query.plan.execute());
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
        boolean stop = false;
        Iterator<Triple> it = p.patterns.iterator();
        while (!stop && it.hasNext()) {
            Triple pt = it.next();
            Iterator<Triple> ittpqs = this.patterns.iterator();
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

