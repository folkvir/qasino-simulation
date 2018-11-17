package snob.simulation.snob2.pipeline;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingProject;
import org.apache.jena.sparql.serializer.SerializationContext;

import java.util.List;

public class ProjectionIterator implements QueryIteratorPlus {
    private QueryIteratorPlus source;
    private List<Var> variables;

    public ProjectionIterator(QueryIteratorPlus source, List<Var> variables) {
        this.source = source;
        this.variables = variables;
    }

    @Override
    public List<Var> getVars() {
        return source.getVars();
    }

    @Override
    public Binding nextBinding() {
        return new BindingProject(variables, source.next());
    }

    @Override
    public void cancel() {
        // we cannot cancel a streaming pipeline
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public Binding next() {
        return nextBinding();
    }

    @Override
    public void output(IndentedWriter out) {
        out.printf(variables.toString());
        source.output(out);
    }

    @Override
    public void close() {
        // we cannot close a streaming pipeline
    }

    @Override
    public void output(IndentedWriter out, SerializationContext sCxt) {
        out.printf(variables.toString());
        source.output(out, sCxt);
    }

    @Override
    public String toString(PrefixMapping pmap) {
        return "Projection(vars=" + variables.toString() + ",source=" + source.toString() + ")";
    }
}
