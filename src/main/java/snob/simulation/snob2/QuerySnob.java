package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;
import snob.simulation.snob2.data.IBFStrata;

import javax.xml.transform.Result;
import java.rmi.ServerError;
import java.util.*;

public class QuerySnob {
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
    public int globalseen = 0;

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
        System.err.printf("[query] %s is finished. %n", query);
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
        if(!this.alreadySeen.containsKey(pattern)) {
            this.alreadySeen.put(pattern, new HashSet<>());
        }
        if(!this.alreadySeen.get(pattern).contains(ours)) this.alreadySeen.get(pattern).add(ours);
        this.alreadySeen.get(pattern).add(remote);
        this.computeGlobalSeen();
    }

    private void computeGlobalSeen() {
        boolean first = false;
        int count = 0;
        for (Set<Integer> set : this.alreadySeen.values()) {
            if(!first) {
                count = set.size();
                first = true;
            } else {
                count = Math.min(count, set.size());
            }
            globalseen = count;
        }
    }

    public void mergeAlreadySeen(Triple pattern, Set<Integer> remote) {
        if(pattern == null || remote == null) return;
        if(!this.alreadySeen.containsKey(pattern)) {
            this.alreadySeen.put(pattern, new HashSet<>());
        }
        this.alreadySeen.get(pattern).addAll(remote);
    }


    public void execute() {
        System.err.printf("Executing a query ... (%d/%d) %s [", this.getResults().size(), this.cardinality, this.query);
        ResultSet res;
        if(plan.results == null) {
            res = plan.execute();
        } else {
            res = plan.results;
        }
        while(res.hasNext()) {
            finalResults.add(res.next());
        }
        System.err.printf("] *end* %n");
    }
}
