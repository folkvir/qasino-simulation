package snob.simulation.snob2.pipeline;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformBase;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PipelineBuilder {
    private Map<Triple, AppendableSource> sources;
    private List<Triple> triples;

    public PipelineBuilder() {
        sources = new HashMap<>();
        triples = new LinkedList<>();
    }

    public QueryIteratorPlus create(String query) {
        Query q = QueryFactory.create(query);
        Op queryTree = Algebra.compile(q);
        PipelineTransformer transformer = new PipelineTransformer(sources, triples);
        Transformer.transform(transformer, queryTree);
        return transformer.getPipeline();
    }

    public Map<Triple, AppendableSource> getSources() {
        return sources;
    }

    public List<Triple> getTriples() {
        return triples;
    }

    private static class PipelineTransformer extends TransformBase {
        private QueryIteratorPlus pipeline;
        private ExecutionContext context;
        private Map<Triple, AppendableSource> sources;
        private List<Triple> triples;

        public PipelineTransformer(Map<Triple, AppendableSource> sources, List<Triple> triples) {
            super();
            this.sources = sources;
            this.triples = triples;
            pipeline = null;
            context = new ExecutionContext(null, null, null, null);
        }

        public QueryIteratorPlus getPipeline() {
            return pipeline;
        }

        private List<Var> getVariables(Triple t) {
            List<Var> vars = new LinkedList<>();
            if (t.getSubject().isVariable()) {
                vars.add((Var) t.getSubject());
            }
            if (t.getPredicate().isVariable()) {
                vars.add((Var) t.getPredicate());
            }
            if (t.getObject().isVariable()) {
                vars.add((Var) t.getObject());
            }
            return vars;
        }

        private Var computeJoinKey(List<Var> left, List<Var> right) {
            for (Var vl : left) {
                for (Var vr : right) {
                    if (vl.equals(vr)) {
                        return vl;
                    }
                }
            }
            return null;
        }

        @Override
        public Op transform(OpProject opProject, Op subOp) {
            pipeline = new ProjectionIterator(pipeline, opProject.getVars());
            return super.transform(opProject, subOp);
        }

        @Override
        public Op transform(OpDistinct opDistinct, Op subOp) {
            pipeline = new DistinctIterator(pipeline);
            return super.transform(opDistinct, subOp);
        }

        @Override
        public Op transform(OpBGP opBGP) {
            opBGP.getPattern().forEach(triple -> {
                triples.add(triple);
                if (pipeline == null) {
                    AppendableSource source = new AppendableSource(triple, getVariables(triple));
                    sources.put(triple, source);
                    pipeline = source;
                } else {
                    AppendableSource right = new AppendableSource(triple, getVariables(triple));
                    sources.put(triple, right);
                    Var joinKey = computeJoinKey(pipeline.getVars(), right.getVars());
                    pipeline = new SymmetricHashJoin(joinKey, pipeline, right, context);
                }
            });
            return super.transform(opBGP);
        }
    }
}
