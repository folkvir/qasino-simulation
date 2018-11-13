package snob.simulation.snob;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

import java.util.*;

public class Profile {
    public int WEIGH_EQUIVALENCE = Integer.MAX_VALUE;
    public int WEIGH_CONTAINMENT = 2;
    public int WEIGH_SUBSET = 1;

    public List<Triple> tpqs;
    public Map<UUID, QuerySnob> queries;
    public long qlimit = 1; // number of queries in the network
    // Datastore
    public Datastore datastore;

    public Profile() {
        this.tpqs = new ArrayList<>();
        this.queries = new HashMap<>();
        this.datastore = new Datastore();
    }

    public void update(String query) {
        System.err.println("Updating the profile with: " + query);
        UUID id = UUID.randomUUID();
        QuerySnob q = new QuerySnob(query);
        queries.put(id, q);
        ElementWalker.walk(q.getQuery().getQueryPattern(), new ElementVisitorBase() {
            @Override
            public void visit(ElementPathBlock elementTriplesBlock) {
                Iterator<TriplePath> it = elementTriplesBlock.patternElts();
                while(it.hasNext()) {
                    tpqs.add(it.next().asTriple());
                }
            }
        });
    }

    public void update(String query, long card) {
        System.err.println("Updating the profile with: " + query);
        UUID id = UUID.randomUUID();
        QuerySnob q = new QuerySnob(query, card);
        queries.put(id, q);
        ElementWalker.walk(q.getQuery().getQueryPattern(), new ElementVisitorBase() {
            @Override
            public void visit(ElementPathBlock elementTriplesBlock) {
                Iterator<TriplePath> it = elementTriplesBlock.patternElts();
                while(it.hasNext()) {
                    tpqs.add(it.next().asTriple());
                }
            }
        });
    }

    public void executeAll() {
        // this.queries.forEach((k,v)->System.err.println(k.toString() + v.toString()));
        // System.err.println("Executing all queries [" + this.queries.values().size() + "]...");
        Iterator<Map.Entry<UUID, QuerySnob>> it = this.queries.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry e = it.next();
            ResultSet res = this.datastore.select(((QuerySnob) e.getValue()).getQuery());
            this.queries.get(e.getKey()).results = res;
        }
    }

    /**
     * Score the provided profile among us
     * high value means that the profile is very interesting
     * @param p the profile to compare with.
     * @return a score based triple pattern containment
     */
    public int score(Profile p) {
        int score = 0;
        boolean stop = false;
        Iterator<Triple> it = p.tpqs.iterator();
        while(!stop && it.hasNext()) {
            Triple pt = it.next();
            Iterator<Triple> ittpqs = this.tpqs.iterator();
            while(!stop && ittpqs.hasNext()) {
                Triple us = ittpqs.next();
                if (this.equivalence(us, pt)) {
                    stop = true; // we have the highest score, stop or it will cause an overflow
                } else if (this.containment(us, pt)) {
                    try {
                        score = Math.addExact(score, WEIGH_CONTAINMENT);
                    } catch (ArithmeticException e) {
                        stop = true;
                    }
                } else if (this.subset(us, pt)) {
                    try {
                        score = Math.addExact(score, WEIGH_SUBSET);
                    } catch (ArithmeticException e) {
                        stop = true;
                    }
                }
            }
        }
        if(stop) {
            return WEIGH_EQUIVALENCE;
        }
        return score;
    }

    public boolean equivalence(Triple tpa, Triple tpb) {
        return tpa.equals(tpb);
    }

    public boolean containment(Triple tpa, Triple tpb) {
        return this.contain(tpa.getSubject(), tpb.getSubject()) &&
                contain(tpa.getPredicate(), tpb.getPredicate()) &&
                contain(tpa.getObject(), tpb.getObject());
    }

    public boolean subset(Triple tpa, Triple tpb) {
        return this.sub(tpa.getSubject(), tpb.getSubject()) &&
                sub(tpa.getPredicate(), tpb.getPredicate()) &&
                sub(tpa.getObject(), tpb.getObject());
    }

    public boolean contain(Node v1, Node v2) {
       return this.eq(v1, v2) || ( !v1.isVariable() && v2.isVariable());
    }

    public boolean sub(Node v1, Node v2) {
        return this.eq(v1, v2) || ( v1.isVariable() && !v2.isVariable());
    }

    public boolean eq(Node v1, Node v2) {
        return v1.equals(v2);
    }
}

