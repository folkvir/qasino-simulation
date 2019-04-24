package qasino.simulation.qasino;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import qasino.simulation.qasino.pipeline.AppendableSource;
import qasino.simulation.qasino.pipeline.PipelineBuilder;
import qasino.simulation.qasino.pipeline.QueryIteratorPlus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryPlan {

    public String query;
    public List<Triple> patterns;
    public Map<Triple, AppendableSource> sources;
    public Map<Triple, Integer> inserted = new HashMap<>();
    public QueryIteratorPlus iterator;
    public List<String> vars;
    public ResultSet results;

    public QueryPlan(String query) {
        this.query = query;
        // build the pipeline
        PipelineBuilder builder = new PipelineBuilder();
        // create the pipeline iterator from the pipeline
        iterator = builder.create(this.query);

        sources = builder.getSources();
        sources.forEach((pattern, source) -> {
            inserted.put(pattern, 0);
        });
        patterns = builder.getTriples();

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

    public void insertTriple(Triple pattern, Triple triple) {
        Binding b = projection(pattern, triple);
        inserted.put(pattern, inserted.get(pattern) + 1);
        sources.get(pattern).append(b);
    }

    public ResultSet execute() {
        try {
            if (results == null) {
                results = ResultSetFactory.create(iterator, vars);
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
