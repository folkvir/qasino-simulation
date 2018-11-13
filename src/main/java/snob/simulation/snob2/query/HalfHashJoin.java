package snob.simulation.snob2.query;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterNullIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.serializer.SerializationContext;

import java.util.List;

public class HalfHashJoin implements QueryIterator {
    private QueryIterator source;
    private QueryIterator currentIter;
    private Var joinKey;
    private HashJoinTable innerTable;
    private HashJoinTable outerTable;
    private ExecutionContext context;

    public HalfHashJoin(Var joinKey, QueryIterator source, HashJoinTable innerTable, HashJoinTable outerTable, ExecutionContext cxt) {
        this.source = source;
        currentIter = new QueryIterNullIterator(context);
        this.joinKey = joinKey;
        this.innerTable = innerTable;
        this.outerTable = outerTable;
        this.context = cxt;
    }


    protected QueryIterator nextStage(Binding binding) {
        if (!binding.contains(joinKey)) {
            return new QueryIterNullIterator(context);
        }
        String key = binding.get(joinKey).toString();

        // insert into inner table
        innerTable.put(key, binding);

        // probe outer table
        List<Binding> joinSolutions = outerTable.probe(key, binding);
        if (joinSolutions.isEmpty()) {
            return new QueryIterNullIterator(context);
        }
        return new QueryIterPlainWrapper(joinSolutions.iterator());
    }

    @Override
    public Binding nextBinding() {
        while (!currentIter.hasNext() && source.hasNext()) {
            currentIter = nextStage(source.next());
        }
        return currentIter.next();
    }

    @Override
    public void cancel() {
        // we cannot cancel a streaming pipeline
    }

    @Override
    public boolean hasNext() {
        if (currentIter.hasNext()) {
            return true;
        }
        while (!currentIter.hasNext() && source.hasNext()) {
            currentIter = nextStage(source.next());
        }
        return currentIter.hasNext();
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
        return "HalfHashJoin(" + source.toString(pmap) + ")";
    }
}
