package snob.simulation.snob2.pipeline;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.serializer.SerializationContext;

import java.util.ArrayList;
import java.util.List;

public class SymmetricHashJoin implements QueryIteratorPlus {
    private Var joinKey;
    private UnionIterator source;
    private HashJoinTable leftTable;
    private HashJoinTable rightTable;
    private List<Var> vars;

    public SymmetricHashJoin(Var joinKey, QueryIteratorPlus left, QueryIteratorPlus right, ExecutionContext cxt) {
        this.joinKey = joinKey;
        leftTable = new HashJoinTable();
        rightTable = new HashJoinTable();
        QueryIterator leftOp = new HalfHashJoin(joinKey, left, leftTable, rightTable, cxt);
        QueryIterator rightOp = new HalfHashJoin(joinKey, right, rightTable, leftTable, cxt);
        source = new UnionIterator(leftOp, rightOp);
        vars = new ArrayList<>();
        vars.addAll(left.getVars());
        vars.addAll(right.getVars());
    }

    @Override
    public List<Var> getVars() {
        return vars;
    }

    @Override
    public void output(IndentedWriter out, SerializationContext sCxt) {
        source.output(out, sCxt);
    }

    @Override
    public String toString(PrefixMapping pmap) {
        return "SymmetricHashJoin(" + source.toString(pmap) + ")";
    }

    @Override
    public Binding nextBinding() {
        return source.next();
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
        return this.nextBinding();
    }

    @Override
    public void output(IndentedWriter out) {
        source.output(out);
    }

    @Override
    public void close() {
        // we cannot close a streaming pipeline
    }
}
