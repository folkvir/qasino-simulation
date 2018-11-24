package snob.simulation.snob2.pipeline;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

import java.util.*;

public class HashJoinTable {
    private Map<String, List<Binding>> content;

    public HashJoinTable() {
        content = new HashMap<>();
    }

    public void put(String key, Binding bindings) {
        if (!content.containsKey(key)) {
            content.put(key, new ArrayList<>());
        }
        List<Binding> old = content.get(key);
        old.add(bindings);
        content.put(key, old);
    }

    public Iterator<Binding> probe(String key, Binding bindings) {
        System.err.printf(".");
        if (content.containsKey(key)) {
            return content.get(key)
                    .parallelStream()
                    .map(b -> {
                        BindingHashMap current = new BindingHashMap();
                        current.addAll(bindings);
                        current.addAll(b);
                        return (Binding) current;
                    }).iterator();
        }
        return Collections.emptyIterator();
    }

    public Iterator<Binding> cartesianProduct(Binding bindings) {
        return content.entrySet()
                .parallelStream()
                .flatMap(entry -> entry.getValue().stream())
                .map(b -> {
                    BindingHashMap current = new BindingHashMap();
                    current.addAll(bindings);
                    current.addAll(b);
                    return (Binding) current;
                }).iterator();
    }
}
