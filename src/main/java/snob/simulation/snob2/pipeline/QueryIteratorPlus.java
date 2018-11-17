package snob.simulation.snob2.pipeline;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;

import java.util.List;

public interface QueryIteratorPlus extends QueryIterator {
    List<Var> getVars();
}
