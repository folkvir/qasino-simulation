package qasino.simulation.snob2.pipeline;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.serializer.SerializationContext;

public class UnionIterator implements QueryIterator {
    private QueryIterator left;
    private QueryIterator right;

    public UnionIterator(QueryIterator left, QueryIterator right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Binding nextBinding() {
        if (left.hasNext()) {
            return left.next();
        }
        return right.next();
    }

    @Override
    public void cancel() {
        // we cannot cancel a streaming pipeline
    }

    @Override
    public boolean hasNext() {
        return left.hasNext() || right.hasNext();
    }

    @Override
    public Binding next() {
        return nextBinding();
    }

    @Override
    public void output(IndentedWriter out) {
        left.output(out);
        right.output(out);
    }

    @Override
    public void close() {
        // we cannot close a streaming pipeline
    }

    @Override
    public void output(IndentedWriter out, SerializationContext sCxt) {
        left.output(out, sCxt);
        right.output(out, sCxt);
    }

    @Override
    public String toString(PrefixMapping pmap) {
        return "left=" + left.toString(pmap) + ", right=" + right.toString(pmap);
    }
}
