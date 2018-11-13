package snob.simulation.snob;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.util.FileManager;

import java.util.Iterator;
import java.util.List;

public class Datastore {
    public Dataset dataset;

    public Datastore() {
        this.dataset = DatasetFactory.createTxnMem();
    }

    public void update(String filename) {
        System.err.println("Updating the datastore with the following filename: "+filename);
        this.dataset.begin(ReadWrite.WRITE);
        try {
            Model tdb = loadModel(filename, this.dataset);
            this.dataset.commit();
        } catch(Exception e) {
            e.printStackTrace();
            this.dataset.abort();
        }
        this.dataset.end();
    }

    public void insertTriples(List<Triple> triples) {
        this.dataset.begin(ReadWrite.WRITE);
        try {
            Iterator<Triple> it = triples.iterator();
            while(it.hasNext()) {
                Triple p = it.next();
                if(!this.dataset.asDatasetGraph().getDefaultGraph().contains(p)) {
                    // System.err.println("Inserting triple: " + p.toString());
                    this.dataset.asDatasetGraph().getDefaultGraph().add(p);
                }
            }
            this.dataset.commit();
        } catch(Exception e) {
            e.printStackTrace();
            this.dataset.abort();
        }
        this.dataset.end();
    }

    public Iterator<Triple> getTriplesMatchingTriplePattern(Triple p) {
        Iterator<Triple> result;
        try{
            this.dataset.begin(ReadWrite.READ);
            Model tdb = this.dataset.getDefaultModel();

            // build bgp and where clause
            BasicPattern bgp = new BasicPattern();
            bgp.add(p);
            //ElementGroup where = new ElementGroup();
            ElementTriplesBlock where = new ElementTriplesBlock();
            //block.addTriple(p);
            where.addTriple(p);

            Query query = QueryFactory.make();
            query.setQueryConstructType();
            query.setConstructTemplate(new Template(bgp));
            query.setQueryPattern(where);


            QueryExecution qe = QueryExecutionFactory.create(query, tdb);
            result = qe.execConstructTriples();

            //qe.close();
        } catch(Exception e) {
            throw e;
        } finally {
            this.dataset.end();
        }
        return result;
    }

    public ResultSet select(Query q) {
        ResultSet result;
        try{
            this.dataset.begin(ReadWrite.READ);
            Model tdb = this.dataset.getDefaultModel();
            QueryExecution qe = QueryExecutionFactory.create(q, tdb);
            result = qe.execSelect();
        } catch(Exception e) {
            throw e;
        } finally {
            this.dataset.end();
        }
        return result;
    }

    public Model loadModel(String filename, Dataset dataset) {
        FileManager.get().readModel(dataset.getDefaultModel(), filename, "RDF/XML" );
        return dataset.getDefaultModel();
    }
}
