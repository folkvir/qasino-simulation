package snob.simulation.snob;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;

public class QuerySnob {
    public String query;
    public Query realQuery;
    public long cardinality;
    public ResultSet results;

    public Query getQuery() {
        return realQuery;
    }

    public QuerySnob(JSONObject json) {
        this.cardinality = (long) json.get("card");
        this.query = (String) json.get("query");
        this.realQuery = QueryFactory.create(this.query);
    }

    public QuerySnob(String query) {
        this.cardinality = 0;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
    }

    public QuerySnob(String query, long card) {
        this.cardinality = card;
        this.query = query;
        this.realQuery = QueryFactory.create(this.query);
    }

    @Override
    public String toString() {
        return "Query: " + this.query + " Cardinality: " + this.cardinality;
    }
}
