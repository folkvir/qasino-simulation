package qasino.simulation.snob2.pipeline;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.serializer.SerializationContext;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * A QueryIterator that yields bindings. Can be augmented with new Binding during query execution by calling the "append" method.
 *
 * @author Thomas Minier
 */
public class AppendableSource implements QueryIteratorPlus {
    private Triple triple;
    private List<Var> vars;
    private Deque<Binding> buffer;

    public AppendableSource(Triple triple, List<Var> vars) {
        this.triple = triple;
        this.vars = vars;
        buffer = new LinkedList<>(); // LinkedList because we just add and remove
    }

    public Triple getTriple() {
        return triple;
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    public void append(Binding b) {
        buffer.addLast(b);
    }

    @Override
    public void output(IndentedWriter out, SerializationContext sCxt) {
        out.printf(buffer.toString());
    }

    @Override
    public String toString(PrefixMapping pmap) {
        return "AppendableSource(" + triple.toString() + ")";
    }

    @Override
    public Binding nextBinding() {
        return buffer.pop();
    }

    @Override
    public void cancel() {
        // we cannot cancel a streaming pipeline
    }

    @Override
    public boolean hasNext() {
        return !buffer.isEmpty();
    }

    @Override
    public Binding next() {
        return nextBinding();
    }

    @Override
    public void output(IndentedWriter out) {
        out.printf(triple.toString());
    }

    @Override
    public void close() {
        // we cannot close a streaming pipeline
    }
}
