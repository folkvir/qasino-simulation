package snob.simulation.snob2;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.apache.jena.sparql.util.ResultSetUtils;
import org.json.simple.JSONObject;

import javax.xml.transform.Result;

public class QuerySnob {
    public String query;
    public Query realQuery;
    public long cardinality;
    public QueryPlan plan;
    public ResultSet results;

    public Query getQuery() {
        return realQuery;
    }

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

    @Override
    public String toString() {
        return "Query: " + this.query + " Cardinality: " + this.cardinality;
    }
}
