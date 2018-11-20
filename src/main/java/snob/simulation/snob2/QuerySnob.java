package snob.simulation.snob2;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.binding.Binding;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class QuerySnob {
    public String query;
    public Query realQuery;
    public long cardinality;
    public QueryPlan plan;
    public List<QuerySolution> finalResults = new LinkedList<>();

    public QuerySnob(JSONObject json) {
        this.cardinality = (long) json.get("card");
        this.query = (String) json.get("query");
        this.realQuery = QueryFactory.create(this.query);
        plan = new QueryPlan(query);
        // populate the bloom filter
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

    public Query getQuery() {
        return realQuery;
    }

    public List<QuerySolution> getResults () {
        return this.finalResults;
    }

    @Override
    public String toString() {
        return "Query: " + this.query + " Cardinality: " + this.cardinality;
    }

    public void insertResults(ResultSet execute) {
        while(execute.hasNext()){
            finalResults.add(execute.next());
        }
    }
}
