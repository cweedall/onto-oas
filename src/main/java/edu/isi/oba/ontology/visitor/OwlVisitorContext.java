package edu.isi.oba.ontology.visitor;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/** Public read-only view of {@link VisitorContext} for use in helper classes. */
public interface OwlVisitorContext {
	OWLOntology getBaseClassOntology();

	OWLClass getBaseClass();

	Schema getClassSchema();

	Set<OWLClass> getReferencedClasses();

	Map<String, Schema> getBasePropertiesMap();

	/**
	 * Returns the set of required property names for the base class.
	 *
	 * @return a set of required property names
	 */
	Set<String> getRequiredProperties();

	Set<OWLClass> getProcessedClasses();

	Set<OWLClass> getProcessedRestrictionClasses();

	boolean isRestrictionClassProcessed(OWLClass cls);

	Map<String, Map<String, String>> getMarkdownGenerationMap();

	Set<String> getPropertyNames();

	Set<String> getEnumProperties();

	Set<String> getFunctionalProperties();

	String getCurrentlyProcessedPropertyName();

	YamlConfig getConfigData();

	OWLReasoner getReasoner();

	OWLClass getOwlThing();

	// Add more getters as needed
}
