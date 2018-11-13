package snob.simulation.snob2.query;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HashJoinTable {
    private Map<String, List<Binding>> content;

    public HashJoinTable() {
        content = new HashMap<>();
    }

    public void put(String key, Binding bindings) {
        if (!content.containsKey(key)) {
            content.put(key, new LinkedList<>());
        }
        List<Binding> old = content.get(key);
        old.add(bindings);
        content.put(key, old);
    }

    public List<Binding> probe(String key, Binding bindings) {
        List<Binding> solutions = new LinkedList<>();
        if (content.containsKey(key)) {
            List<Binding> matches = content.get(key);
            for(Binding b: matches) {
                BindingHashMap current = new BindingHashMap();
                current.addAll(bindings);
                current.addAll(b);
                solutions.add(current);
            }
        }
        return solutions;
    }
}
