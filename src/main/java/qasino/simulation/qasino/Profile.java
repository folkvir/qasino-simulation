package qasino.simulation.qasino;

import org.apache.jena.graph.Triple;

import java.util.LinkedList;
import java.util.List;

public class Profile {
    public int inserted = 0;
    public boolean has_query = false;
    public long replicate = 50; // replicate factor in % (one query is replicated over a limited number of peer, 'replicate is this number)

    public QueryQasino query;
    public Datastore local_datastore = new Datastore();


    /**
     * Insert triple into the datastore from a pattern and a list of triples matching this triple pattern.
     *
     * @param pattern
     * @param list
     * @return
     */
    public int insertLocalTriplesWithList(Triple pattern, List<Triple> list) {
        inserted += list.size();
        List<Triple> ibf = new LinkedList<>();
        for (Triple triple : list) {
            //if (!local_datastore.contains(triple)) {
            // System.err.println("Insert in the database and pipeline: " + triple);
            ibf.add(triple);
            query.insertTriple(pattern, triple);
            //}
        }
        local_datastore.insertTriples(ibf);
        return list.size();
    }

    /**
     * Insert triple into the datastore from a pattern and a list of triples matching this triple pattern.
     *
     * @param pattern
     * @param list
     * @return
     */
    public int insertTriplesWithList(Triple pattern, List<Triple> list) {
        inserted += list.size();
        List<Triple> ibf = new LinkedList<>();
        for (Triple triple : list) {
            if (!query.data.get(pattern).contains(triple)) {
                // System.err.println("Insert in the database and pipeline: " + triple);
                ibf.add(triple);
                query.insertTriple(pattern, triple);
            }
        }
        return ibf.size();
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
            this.query = new QueryQasino(query);
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
            this.query = new QueryQasino(query, card);
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
        //System.err.println("[INIT] Initializing the pipeline...");
        for (Triple pattern : patterns) {
            // System.err.printf("[INIT] Inserting triples from %s into the pipeline: ", pattern.toString());
            this.insertTriplesWithList(pattern, this.local_datastore.getTriplesMatchingTriplePatternAsList(pattern));
            // System.err.println(pattern + ": " + this.local_datastore.inserted + " - " + this.query.datastore.inserted);
        }
    }

    /**
     * Stop the query execution;
     */
    public void stop() {
        this.query.stop();
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
}

