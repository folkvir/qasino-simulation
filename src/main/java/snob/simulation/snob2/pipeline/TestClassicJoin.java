package snob.simulation.snob2.pipeline;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class TestClassicJoin {

    public static void main(String[] args) {
        String query = "prefix yago: <http://dbpedia.org/class/yago/>" +
                "SELECT *\n" +
                "WHERE {\n" +
                "  ?person a yago:Carpenters, yago:PeopleExecutedByCrucifixion.\n" +
                "}";
        PipelineBuilder builder = new PipelineBuilder();
        QueryIteratorPlus iterator = builder.create(query);
        Map<Triple, AppendableSource> sources = builder.getSources();
        List<Triple> triples = builder.getTriples();


        BindingHashMap b1 = new BindingHashMap();
        b1.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#toto"));
        b1.add(Var.alloc("label"), NodeFactory.createLiteral("\"toto\""));

        BindingHashMap b2 = new BindingHashMap();
        b2.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#titi"));
        b2.add(Var.alloc("label"), NodeFactory.createLiteral("\"titi\""));

        BindingHashMap b3 = new BindingHashMap();
        b3.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#toto"));
        b3.add(Var.alloc("country"), NodeFactory.createLiteral("\"France\""));

        BindingHashMap b4 = new BindingHashMap();
        b4.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#tata"));
        b4.add(Var.alloc("country"), NodeFactory.createLiteral("\"USA\""));

        BindingHashMap b5 = new BindingHashMap();
        b5.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#toto"));
        b5.add(Var.alloc("country"), NodeFactory.createLiteral("\"Bretagne\""));

        BindingHashMap b6 = new BindingHashMap();
        b6.add(Var.alloc("person"), NodeFactory.createURI("http://example.org#toto"));
        b6.add(Var.alloc("label"), NodeFactory.createLiteral("\"tyty\""));

        sources.get(triples.get(0)).append(b1);
        sources.get(triples.get(0)).append(b2);

        sources.get(triples.get(1)).append(b3);
        sources.get(triples.get(1)).append(b4);
        sources.get(triples.get(1)).append(b5);

        System.err.println("1st execution");

        try {
            while (iterator.hasNext()) {
                System.err.println(iterator.next());
            }
        } catch (NoSuchElementException e) {

        }

        System.err.println("-------");
        System.err.println("Re-executing");

        sources.get(triples.get(0)).append(b6);

        try {
            while (iterator.hasNext()) {
                System.err.println(iterator.next());
            }
        } catch (NoSuchElementException e) {

        }
    }
}
