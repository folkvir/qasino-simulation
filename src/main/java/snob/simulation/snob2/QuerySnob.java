package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;
import snob.simulation.snob2.data.IBFStrata;

import java.util.*;

import static java.lang.System.exit;

public class QuerySnob {
    // ids
    private static int queryids = 1;
    public final int qid = QuerySnob.queryids++;

    // for the class
    public boolean terminated = false;
    public String query;
    public Query realQuery;
    public long cardinality;
    public QueryPlan plan;
    public List<QuerySolution> finalResults = new LinkedList<>();
    public List<Triple> patterns = new ArrayList<>();
    public Map<Triple, Set<Triple>> data = new HashMap<>();
    public Map<Triple, IBFStrata> strata = new HashMap<>();
    public Map<Triple, Set<Integer>> alreadySeen = new HashMap<>();
    // stats
    public int globalseen = 0;
    public int executionNumber = 0;
    public boolean tripleInserted = false;
    public int numberOfTriplesInserted = 0;
    public int numberOfTriplesInsertedByround = 0;

    public QuerySnob(JSONObject json) {
        this.cardinality = (long) json.get("card");
        this.query = (String) json.get("query");
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
    }

    public QuerySnob(String query) {
        this.cardinality = 0;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
    }

    public QuerySnob(String query, long card) {
        this.cardinality = card;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
    }

    public void stop() {
        System.err.printf("[query-%d] %s is finished. %n", qid, query);
        terminated = true;
    }

    public Query getQuery() {
        return realQuery;
    }

    public List<QuerySolution> getResults() {
        return this.finalResults;
    }

    @Override
    public String toString() {
        return "Query: " + this.query + " Cardinality: " + this.cardinality;
    }

    public void addAlreadySeen(Triple pattern, int remote, int ours) {
        if (!this.alreadySeen.containsKey(pattern)) {
            this.alreadySeen.put(pattern, new HashSet<>());
        }
        if (!this.alreadySeen.get(pattern).contains(ours)) this.alreadySeen.get(pattern).add(ours);
        this.alreadySeen.get(pattern).add(remote);
        this.computeGlobalSeen();
    }

    private void computeGlobalSeen() {
        boolean first = false;
        int count = 0;
        for (Set<Integer> set : this.alreadySeen.values()) {
            if (!first) {
                count = set.size();
                first = true;
            } else {
                count = Math.min(count, set.size());
            }
            globalseen = count;
        }
    }

    public void mergeAlreadySeen(Triple pattern, Set<Integer> remote) {
        if (pattern == null || remote == null) return;
        if (!this.alreadySeen.containsKey(pattern)) {
            this.alreadySeen.put(pattern, new HashSet<>());
        }
        this.alreadySeen.get(pattern).addAll(remote);
    }


    public void execute() {
        System.err.printf("[query-%d]Executing a query ... (%d/%d) %s [ %n ** Executing... %n ", qid, this.getResults().size(), this.cardinality, this.query);
        ResultSet res;
        executionNumber++;
        if (plan.results == null) {
            System.err.printf("** (First execution) ");
            res = plan.execute();
        } else {
            System.err.printf("** (%d-th execution) %n ", executionNumber);
            res = plan.results;
        }
        System.err.printf("** (has new triples? = %b) (T_N_OF_T_I = %d) (T_N_OF_T_I_B_R = %d) %n ", tripleInserted, numberOfTriplesInserted, numberOfTriplesInsertedByround);
        if(tripleInserted && res.hasNext()) {
            System.err.println("** Pipeline has pending results...");
            while (res.hasNext()) {
                QuerySolution sol = res.next();
                System.err.printf("** Adding result %s to the static final results set. %n", sol);
                finalResults.add(sol);
            }
            tripleInserted = false;
            numberOfTriplesInsertedByround = 0;
        } else {
            System.err.println("** No triple inserted and/or pipeline has no pending result, stop...");
        }
        System.err.println("Final results set, number of results: " + finalResults.size() + " out of: " + cardinality);
        if (finalResults.size() > this.cardinality) {
            System.err.println(new Exception("too much results compared to the cardinality of the query."));
            exit(1);
        }
        System.err.printf("] *end* %n");
    }

    public void insertTriple(Triple pattern, Triple t) {
        if(terminated) {
            throw new Error("query already terminated");
        } else {
            if(!plan.patterns.contains(pattern)) {
                throw new Error("Pattern does not exist in the query.");
            } else {
                numberOfTriplesInserted++;
                numberOfTriplesInsertedByround++;
                tripleInserted = true;
                plan.insertTriple(pattern, t);
            }
        }
    }
}
