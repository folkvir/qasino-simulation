package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;
import peersim.core.Network;
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
    public Map<Triple, Set<Triple>> data = new LinkedHashMap<>();
    public Map<Triple, IBFStrata> strata = new LinkedHashMap<>();
    public Map<Triple, Set<Integer>> alreadySeen = new LinkedHashMap<>();
    // stats
    public Set<Integer> seen = new LinkedHashSet<>();
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
        patterns = plan.patterns;
        data = new LinkedHashMap<>();
        for (Triple pattern : plan.patterns) {
            data.put(pattern, new LinkedHashSet<>());
            strata.put(pattern, new IBFStrata());
        }
    }

    public QuerySnob(String query) {
        this.cardinality = 0;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
        patterns = plan.patterns;
        data = new LinkedHashMap<>();
        for (Triple pattern : plan.patterns) {
            data.put(pattern, new LinkedHashSet<>());
            strata.put(pattern, new IBFStrata());
        }
    }

    public QuerySnob(String query, long card) {
        this.cardinality = card;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
        patterns = plan.patterns;
        data = new LinkedHashMap<>();
        for (Triple pattern : plan.patterns) {
            data.put(pattern, new LinkedHashSet<>());
            strata.put(pattern, new IBFStrata());
        }
    }

    public void stop() {
        // System.err.printf("[query-%d] %s is finished. %n", qid, query);
        terminated = true;
    }

    public boolean isFinished() {
        boolean res = true;
        for (Map.Entry<Triple, Set<Integer>> entry : alreadySeen.entrySet()) {
            if (entry.getValue().size() == Network.size()) {
                res = res && true;
            } else {
                res = false;
            }
        }
        if (alreadySeen.size() == 0) return false;
        return res;
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

    public boolean addAlreadySeen(Triple pattern, int remote, int ours) {
        boolean res = false;
        if (!this.alreadySeen.containsKey(pattern)) {
            this.alreadySeen.put(pattern, new LinkedHashSet<>());
        }
        if (!this.seen.contains(remote)) res = true;
        if (!this.alreadySeen.get(pattern).contains(ours)) res = true;
        this.seen.add(ours);
        this.seen.add(remote);
        this.alreadySeen.get(pattern).add(ours);
        this.alreadySeen.get(pattern).add(remote);
        this.computeGlobalSeen();
        return res;
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
            this.alreadySeen.put(pattern, new LinkedHashSet<>());
        }
        // System.err.println("Merge our view " + this.alreadySeen.get(pattern) + "with " + remote);
        this.alreadySeen.get(pattern).addAll(remote);
        computeGlobalSeen();
        // just a rapid hack, but need to iterate on remote
        this.seen.addAll(remote);
    }


    public void execute() {
        // System.err.printf("[query-%d]Executing a query ... (%d/%d) %s [ %n ** Executing... %n ", qid, this.getResults().size(), this.cardinality, this.query);
        ResultSet res;
        executionNumber++;
        if (plan.results == null) {
            // System.err.printf("** (First execution) ");
            res = plan.execute();
        } else {
            // System.err.printf("** (%d-th execution) %n ", executionNumber);
            res = plan.results;
        }
        // System.err.printf("** (has new triples? = %b) (T_N_OF_T_I = %d) (T_N_OF_T_I_B_R = %d) %n ", tripleInserted, numberOfTriplesInserted, numberOfTriplesInsertedByround);
        if (tripleInserted && res.hasNext()) {
            // System.err.println("** Pipeline has pending results...");
            while (res.hasNext()) {
                QuerySolution sol = res.next();
                // System.err.printf("** Adding result %s to the static final results set. %n", sol);
                finalResults.add(sol);
            }
            tripleInserted = false;
            numberOfTriplesInsertedByround = 0;
        } else {
            // System.err.println("** No triple inserted and/or pipeline has no pending result, stop...");
        }
        // System.err.println("Final results set, number of results: " + finalResults.size() + " out of: " + cardinality);
        if (finalResults.size() > this.cardinality) {
            System.err.println(new Exception("too much results compared to the cardinality of the query."));
            exit(1);
        }
        // System.err.printf("] *end* %n");
    }

    public void insertTriple(Triple pattern, Triple t) {
        if (!plan.patterns.contains(pattern)) {
            throw new Error("Pattern does not exist in the query.");
        } else {
            if (data.get(pattern).contains(t)) {
                throw new Error("Triple already inserted.");
            } else {
                numberOfTriplesInserted++;
                numberOfTriplesInsertedByround++;
                tripleInserted = true;
                data.get(pattern).add(t);
                plan.insertTriple(pattern, t);
                List<Triple> tl = new ArrayList<>();
                tl.add(t);
                strata.get(pattern).insert(tl);
            }
        }
    }

    public boolean probabilisticIsFinished(double apriori, int shuffle) {
        double distinct = this.globalseen;
        if(distinct == 0) return false;
        double infinity = 20;
        double sum = 0;
        for(int i = 0; i < infinity; ++i) {
            double tmp = distinct / (distinct + i);
            double pow = Math.pow(tmp, shuffle);
            // System.err.println(tmp + " : " + pow);
            sum += pow;
        }
        double result = apriori * sum;
        // System.err.println("distinct: " + distinct + "; |" + result + " round=" + shuffle + " stop cond: " + (result < 1));
        return result < 1;
    }
}
