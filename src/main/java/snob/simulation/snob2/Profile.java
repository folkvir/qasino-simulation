package snob.simulation.snob2;


import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.util.ResultSetUtils;

import java.util.*;

public class Profile {
    public int WEIGH_EQUIVALENCE = Integer.MAX_VALUE;
    public int WEIGH_CONTAINMENT = 2;
    public int WEIGH_SUBSET = 1;

    public List<Triple> patterns;
    public QuerySnob query;
    public long qlimit = 1; // number of queries in the network
    // Datastore
    public Datastore datastore;

    public Profile() {
        this.patterns = new ArrayList<>();
        this.datastore = new Datastore();
    }

    public void insertTriples(Map<Triple, Iterator<Triple>> its) {
        its.forEach((pattern, iterator) -> {
            List<Triple> list = new ArrayList<>();
            // consume the iterator and fill the query
            while(iterator.hasNext()) {
                Triple t = iterator.next();
                query.plan.insertTriple(pattern, t);
                list.add(t);
            }
            // insert triples in datastore
            datastore.insertTriples(list);
        });
    }

    public void update(String query) {
        System.err.println("Updating the profile with: " + query);
        UUID id = UUID.randomUUID();
        this.query = new QuerySnob(query);
        patterns = this.query.plan.patterns;
    }

    public void update(String query, long card) {
        System.err.println("Updating the profile with: " + query);
        UUID id = UUID.randomUUID();
        QuerySnob q = new QuerySnob(query, card);
        this.query = new QuerySnob(query);
        patterns = this.query.plan.patterns;
    }

    public void execute () {
        try {
            if(this.query != null) {
                System.err.println("Executing the query: " + this.query.query);
                ResultSet set = this.query.plan.execute();
                if(this.query.results == null) {
                    this.query.results = set;
                } else {
                    this.query.results = ResultSetUtils.union(this.query.results, set);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
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
        Iterator<Triple> it = p.patterns.iterator();
        while(!stop && it.hasNext()) {
            Triple pt = it.next();
            Iterator<Triple> ittpqs = this.patterns.iterator();
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

