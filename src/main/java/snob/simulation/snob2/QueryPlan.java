package snob.simulation.snob2;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import snob.simulation.snob2.query.AppendableSource;
import snob.simulation.snob2.query.PipelineBuilder;
import snob.simulation.snob2.query.QueryIteratorPlus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryPlan {

    public String query;
    public List<Triple> patterns;
    public Map<Triple, AppendableSource> sources;
    public QueryIteratorPlus iterator;
    public List<String> vars;

    public QueryPlan(String query) {
        this.query = query;
        // build the pipeline
        PipelineBuilder builder = new PipelineBuilder();
        // create the query iterator from the query
        iterator = builder.create(this.query);

        sources = builder.getSources();
        patterns = builder.getTriples();
        patterns.forEach(pattern -> {
            System.err.println("Pattern: " + pattern.toString());
        });

        vars = iterator.getVars().parallelStream().map(var -> var.toString()).collect(Collectors.toList());
    }

    private Binding projection(Triple pattern, Triple triple) {
        BindingHashMap b = new BindingHashMap();
        if (pattern.getSubject().isVariable()) {
            b.add((Var) pattern.getSubject(), triple.getSubject());
        }
        if (pattern.getPredicate().isVariable()) {
            b.add((Var) pattern.getPredicate(), triple.getPredicate());
        }
        if (pattern.getObject().isVariable()) {
            b.add((Var) pattern.getObject(), triple.getObject());
        }
        return b;
    }

    public void insertTriple (Triple pattern, Triple triple) {
        Binding b = projection(pattern, triple);
        sources.get(pattern).append(b);
    }

    public ResultSet execute () {
        try {
            ResultSet set = null;
            set = ResultSetFactory.create(iterator, vars);
            return set;
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
