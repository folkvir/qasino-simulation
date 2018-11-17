package snob.simulation.snob2.pipeline;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class TestCartesianProduct {
    public static void main(String[] args) {
        String query = "SELECT DISTINCT ?x1 WHERE { " +
                "?x1 <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseaseSubtypeOf> <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/1130> ." +
                " <http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseases/1130> <http://www.w3.org/2002/07/owl#sameAs> ?x3 }";
        PipelineBuilder builder = new PipelineBuilder();
        QueryIteratorPlus iterator = builder.create(query);
        Map<Triple, AppendableSource> sources = builder.getSources();
        List<Triple> triples = builder.getTriples();

        BindingHashMap b1 = new BindingHashMap();
        b1.add(Var.alloc("x1"), NodeFactory.createURI("http://example.org#toto"));

        BindingHashMap b2 = new BindingHashMap();
        b2.add(Var.alloc("x1"), NodeFactory.createURI("http://example.org#titi"));

        BindingHashMap b3 = new BindingHashMap();
        b3.add(Var.alloc("x3"), NodeFactory.createLiteral("\"France\""));

        BindingHashMap b4 = new BindingHashMap();
        b4.add(Var.alloc("x3"), NodeFactory.createLiteral("\"USA\""));

        sources.get(triples.get(0)).append(b1);
        sources.get(triples.get(0)).append(b2);

        sources.get(triples.get(1)).append(b3);
        sources.get(triples.get(1)).append(b4);

        System.out.println("1st execution");

        try {
            while(iterator.hasNext()) {
                System.out.println(iterator.next());
            }
        } catch(NoSuchElementException e) {

        }
    }
}
