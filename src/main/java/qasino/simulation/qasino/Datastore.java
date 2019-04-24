package qasino.simulation.qasino;

import com.google.common.collect.Lists;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.util.FileManager;

import java.util.Iterator;
import java.util.List;

import static java.lang.System.exit;


public class Datastore {
    public int inserted = 0;
    public Model general;
    public Dataset dataset;

    public Datastore() {
        // this could be optimized...
        this.general = ModelFactory.createDefaultModel();
        this.dataset = DatasetFactory.createTxnMem();
    }

    public void update(String filename) {
        try {
            RDFParser.create()
                    .source(filename)
                    .parse(general);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean contains(Triple p) {
        return general.getGraph().contains(p);
    }

    public void insertTriples(List<Triple> triples) {
        try {
            Graph g = general.getGraph(); // this.dataset.asDatasetGraph().getDefaultGraph();
            Iterator<Triple> it = triples.iterator();
            while (it.hasNext()) {
                Triple p = it.next();
                if (!g.contains(p)) {
                    // System.err.println("Inserting triple: " + p.toString());
                    g.add(p);
                    inserted++;
                } else {
                    System.err.println(new Error("[]inserting twice a triple..."));
                    exit(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Triple> getTriplesMatchingTriplePatternAsList(Triple p) {
        return Lists.newArrayList(this.getTriplesMatchingTriplePattern(p));
    }

    public Iterator<Triple> getTriplesMatchingTriplePattern(Triple p) {
        Iterator<Triple> result;
        try {
            // this.dataset.begin(ReadWrite.READ);
            // Model tdb = this.dataset.getDefaultModel();
            Model tdb = general;

            // build bgp and where clause
            BasicPattern bgp = new BasicPattern();
            bgp.add(p);
            //ElementGroup where = new ElementGroup();
            ElementTriplesBlock where = new ElementTriplesBlock();
            where.addTriple(p);
            // System.err.printf("Triple: %b %b %b %n", p.getSubject().isVariable(), p.getPredicate().isVariable(), p.getObject().isVariable());

            Query query = QueryFactory.make();
            query.setQueryConstructType();
            query.setConstructTemplate(new Template(bgp));
            query.setQueryPattern(where);


            QueryExecution qe = QueryExecutionFactory.create(query, tdb);
            result = qe.execConstructTriples();
            //qe.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // this.dataset.end();
        }
        return result;
    }

    public ResultSet select(Query q) {
        ResultSet result;
        try {
            // this.dataset.begin(ReadWrite.READ);
            Model tdb = general; // this.dataset.getDefaultModel();
            QueryExecution qe = QueryExecutionFactory.create(q, tdb);
            result = qe.execSelect();
        } catch (Exception e) {
            throw e;
        } finally {
            // this.dataset.end();
        }
        return result;
    }

    public Model loadModel(String filename, Dataset dataset) {
        FileManager.get().readModel(dataset.getDefaultModel(), filename, "RDF/XML");
        return dataset.getDefaultModel();
    }
}
