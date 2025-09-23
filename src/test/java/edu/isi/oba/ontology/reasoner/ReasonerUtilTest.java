package edu.isi.oba.ontology.reasoner;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class ReasonerUtilTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<ReasonerUtil> constructor = ReasonerUtil.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	void shouldCreateReasonerAndClassifyOntology_whenOntologyIsValid()
			throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();

		// Create ontology
		OWLOntology ontology = manager.createOntology();

		// Define classes
		OWLClass animal = dataFactory.getOWLClass(IRI.create("http://example.org/Animal"));
		OWLClass mammal = dataFactory.getOWLClass(IRI.create("http://example.org/Mammal"));

		// Add axiom: Mammal is a subclass of Animal
		OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(mammal, animal);
		manager.addAxiom(ontology, axiom);

		// Create reasoner
		OWLReasoner reasoner = ReasonerUtil.createReasoner(ontology);

		// Perform classification
		reasoner.precomputeInferences();

		// Verify classification
		assertTrue(reasoner.getSubClasses(animal, false).containsEntity(mammal));
		assertFalse(reasoner.getSubClasses(mammal, false).containsEntity(animal));
	}
}
