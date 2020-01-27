package net.judah.plugin;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import lombok.extern.log4j.Log4j;

@Log4j
public class Lv2Info {

	public static void main(String[] args) {
		
		log.info("hello world");
		
		Model model = ModelFactory.createDefaultModel();
		
		log.info("default model created");
		
		model.read("/home/jam/lib/tap/echo/tap_echo.ttl") ;
		
		
//        System.out.println("\n---- N-Triples ----");
//        model.write(System.out, "N-TRIPLES");
//        System.out.println("\n---- RDF/JSON ----");
//        model.write(System.out, "RDF/JSON");

		
		// list the statements in the Model
		StmtIterator iter = model.listStatements();

		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object


//		    if (! predicate.toString().equals("http://lv2plug.in/ns/lv2core#port")) continue;

		    System.out.print(subject.toString());
		    System.out.print(" " + predicate.toString() + " ");
		    if (object instanceof Resource) {
//		       System.out.print(object.toString());
		    } else {
		        // object is a literal
		        System.out.print(" \"" + object.toString() + "\"");
		    }

		    System.out.println(" .");
		} 
		
	}
	
}
