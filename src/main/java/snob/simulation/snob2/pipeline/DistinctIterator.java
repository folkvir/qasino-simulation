package snob.simulation.snob2.pipeline;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.serializer.SerializationContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DistinctIterator implements QueryIteratorPlus {
    private QueryIteratorPlus source;
    private Set<String> seenBefore;
    private Optional<Binding> nextValue;

    public DistinctIterator(QueryIteratorPlus source) {
        this.source = source;
        seenBefore = new LinkedHashSet<>();
        nextValue = Optional.empty();
    }

    @Override
    public List<Var> getVars() {
        return source.getVars();
    }

    @Override
    public Binding nextBinding() {
        while (!nextValue.isPresent() && source.hasNext()) {
            Binding b = source.next();
            if (!seenBefore.contains(b.toString())) {
                seenBefore.add(b.toString());
                nextValue = Optional.of(b);
            }
        }
        Binding value = nextValue.get();
        nextValue = Optional.empty();
        return value;
    }

    @Override
    public void cancel() {
        // we cannot cancel a streaming pipeline
    }

    @Override
    public boolean hasNext() {
        while (!nextValue.isPresent() && source.hasNext()) {
            Binding b = source.next();
            if (!seenBefore.contains(b.toString())) {
                seenBefore.add(b.toString());
                nextValue = Optional.of(b);
            }
        }
        return nextValue.isPresent();
    }

    @Override
    public Binding next() {
        return nextBinding();
    }

    @Override
    public void output(IndentedWriter out) {
        source.output(out);
    }

    @Override
    public void close() {
        // we cannot close a streaming pipeline
    }

    @Override
    public void output(IndentedWriter out, SerializationContext sCxt) {
        source.output(out, sCxt);
    }

    @Override
    public String toString(PrefixMapping pmap) {
        return "Distinct(" + source.toString() + ")";
    }
}
