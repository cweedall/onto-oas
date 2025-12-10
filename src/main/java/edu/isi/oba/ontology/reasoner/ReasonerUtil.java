package edu.isi.oba.ontology.reasoner;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

public class ReasonerUtil {
	private static final OWLReasonerFactory factory = new StructuralReasonerFactory();

	private ReasonerUtil() {
		throw new UnsupportedOperationException("Static utility class");
	}

	public static OWLReasoner createReasoner(OWLOntology ontology) {
		return factory.createReasoner(ontology);
	}
}
