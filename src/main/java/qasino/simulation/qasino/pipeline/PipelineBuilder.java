package qasino.simulation.qasino.pipeline;

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

import java.util.*;

public class PipelineBuilder {
    private Map<Triple, AppendableSource> sources;
    private List<Triple> triples;
    private Query query;

    public PipelineBuilder() {
        sources = new LinkedHashMap<>();
        triples = new LinkedList<>();
    }

    public QueryIteratorPlus create(String query) {
        Query q = QueryFactory.create(query);
        this.query = q;
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
            List<Triple> list = null;
            try {
                list = reordering(opBGP.getPattern().getList());
            } catch (Exception e) {
                e.printStackTrace();
            }
            list.forEach(triple -> {
                // System.err.println(triple);
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

        public List<Triple> reordering(List<Triple> opBGPList) {
            Collections.sort(opBGPList, ((o1, o2) -> {
                int one = 0, two = 0;
                // System.err.println(o1 + " _ " + o2);
                int per1m = joinPatternOrderTer(o1, o2);
                int per2m = joinPatternOrderTer(o2, o1);

                int c1 = getConstants(o1);
                int c2 = getConstants(o2);

                int tpo1 = triplePatternOrder(o1, o2);
                int tpo2 = triplePatternOrder(o2, o1);

                one += per1m + c1 + tpo1;
                two += per2m + c2 + tpo2;
                return two - one;
            }));
            return opBGPList;
        }

//        public List<Triple> reorderingPerm(List<Triple> opBGPList) throws Exception {
//            Collection<List<Triple>> permutation = Collections2.permutations(opBGPList);
//            Object[] arr = permutation.toArray();
//            int size = permutation.size();
//            Iterator<List<Triple>> it = permutation.iterator();
//            int[] scores = new int[size];
//            int max = 0;
//            List<Triple> perm = new ArrayList<>();
//            for (int i = 0; i < size; i++) {
//                List<Triple> list = it.next();
//                int listSize = list.size();
//                int score = 0;
//                Triple begin = list.get(0);
//                for(int j = 1; j < listSize; ++j) {
//                    score += getConstants(list.get(j)) + getPlace(list.get(j)) + joinPatternOrderTer(begin, list.get(j));
//                    begin = list.get(j);
//                }
//                scores[i] = score;
//                System.err.println("Permutation: " + list.toString() + " (" + score + ")");
//                if(i == 0) {
//                    max = i;
//                    perm = list;
//                }
//                if(score > scores[max]) {
//                    // System.err.println("score= " + scores[i] + " " + list);
//                    max = i;
//                    perm = list;
//                } else if (score == scores[max]) {
//                    // if the score is the same, order them by
//                }
//            }
//
////            for (int i = 0; i < scores.length; i++) {
////                System.err.println("Perm score: " + scores[i]);
////            }
////            System.err.println("Best score: " + scores[max]);
//            System.err.println("Best perm: " +  arr[max]);
//            List<Triple> result = (List<Triple>)arr[max];
//            return result;
//        }

        private int joinPatternOrderTer(Triple o1, Triple o2) {
            Var joinKey = computeJoinKey(getVariables(o1), getVariables(o2));
            // System.err.println("Joinkey: " + joinKey + "_" );
            if (o1.getPredicate().equals(o2.getObject()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("predicate === object");
                return 6;
            } else if (o1.getSubject().equals(o2.getPredicate()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("subject === predicate");
                return 5;
            } else if (o1.getSubject().equals(o2.getObject()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("subject === object");
                return 4;
            } else if (o1.getObject().equals(o2.getObject()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("object === object");
                return 3;
            } else if (o1.getSubject().equals(o2.getSubject()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("subject === subject");
                return 2;
            } else if (o1.getPredicate().equals(o2.getPredicate()) && o1.getPredicate().equals(joinKey)) {
                // System.err.println("predicate === predicate");
                return 1;
            } else {
                // System.err.println("Nothing case? hum... perhaps there is an error in the code...");
                return 0;
            }
        }

        /**
         * Return a list of patterns ordered by the number of constants,
         * Higher the constant number, smaller the index
         *
         * @param opBGPList
         * @return
         */
        public List<Triple> constantOrdering(List<Triple> opBGPList) {
            Collections.sort(opBGPList, (o1, o2) -> constantOrder(o1, o2));
            return opBGPList;
        }

        /**
         * Order the bgp by selectivity
         *
         * @param opBGPList
         */
        private List<Triple> triplePatternOrdering(List<Triple> opBGPList) {
            Collections.sort(opBGPList, (o1, o2) -> triplePatternOrder(o1, o2));
            return opBGPList;
        }

        private int constantOrder(Triple o1, Triple o2) {
            int constantso1 = getConstants(o1);
            int constantso2 = getConstants(o2);
            if (constantso1 < constantso2) {
                return 1;
            } else if (constantso1 > constantso2) {
                return -1;
            } else {
                return 0;
            }
        }

        private int getConstants(Triple triple) {
            int constants = 0;
            if (!triple.getSubject().isVariable()) {
                constants++;
            }
            if (!triple.getPredicate().isVariable()) {
                constants++;
            }
            if (!triple.getObject().isVariable()) {
                constants++;
            }
            return constants;
        }

        private int triplePatternOrder(Triple o1, Triple o2) {
            int indexo1 = 0;
            try {
                indexo1 = getPlace(o1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int indexo2 = 0;
            try {
                indexo2 = getPlace(o2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (indexo1 < indexo2) {
                return -1;
            } else if (indexo1 > indexo2) {
                return 1;
            } else {
                return 0;
            }
        }

        /**
         * Return the form of the pattern,
         * spo -> s?o -> ?po -> sp? -> ??o -> s?? -> ?p? -> ???
         * 8 -> 7 -> ... 2 -> 1
         *
         * @param o1
         * @return
         */
        private int getPlace(Triple o1) throws Exception {
            if (!o1.getSubject().isVariable() && !o1.getPredicate().isVariable() && !o1.getObject().isVariable() && o1.getPredicate().isURI() && !o1.getPredicate().getURI().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                // spo
                return 8;
            } else if (!o1.getSubject().isVariable() && o1.getPredicate().isVariable() && !o1.getObject().isVariable()) {
                // s?o
                return 7;
            } else if (o1.getSubject().isVariable() && !o1.getPredicate().isVariable() && !o1.getObject().isVariable()) {
                // ?po
                return 6;
            } else if (!o1.getSubject().isVariable() && !o1.getPredicate().isVariable() && o1.getObject().isVariable()) {
                // sp?
                return 5;
            } else if (o1.getSubject().isVariable() && o1.getPredicate().isVariable() && !o1.getObject().isVariable()) {
                // ??o
                return 4;
            } else if (!o1.getSubject().isVariable() && o1.getPredicate().isVariable() && o1.getObject().isVariable()) {
                // s??
                return 3;
            } else if (o1.getSubject().isVariable() && !o1.getPredicate().isVariable() && o1.getObject().isVariable()) {
                // ?p?
                return 2;
            } else if (o1.getSubject().isVariable() && o1.getPredicate().isVariable() && o1.getObject().isVariable()) {
                // ??? except for rdf:type
                return 1;
            } else {
                throw new Exception("cannot classify this pattern: " + o1.toString());
            }
        }
    }
}
