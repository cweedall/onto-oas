package edu.isi.oba.config.paths;

import static edu.isi.oba.Oba.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.IRI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PathsForClassConfig {
	private IRI classIRI;
	private Set<OperationType> allowOperationTypes = new HashSet<>();
	private Set<String> allowOperations = new HashSet<>();
	private Set<OperationType> denyOperationTypes = new HashSet<>();
	private Set<String> denyOperations = new HashSet<>();

	public IRI getClassIRI() {
		return this.classIRI;
	}

	public void setClass_iri(String class_iri_string) {
		this.classIRI = IRI.create(class_iri_string);
	}

	public Set<OperationType> getAllowOperationTypes() {
		return this.allowOperationTypes;
	}

	public void setAllowOperations(Set<String> allowOperations) {
		if (Collections.disjoint(allowOperations, this.denyOperations)) {
			this.allowOperations = allowOperations;
			this.allowOperationTypes =
					allowOperations.stream()
							.map(operation -> OperationType.valueOfLabel(operation))
							.collect(Collectors.toSet());
		} else {
			logger.severe(
					"In the YAML configuration file, no class in `paths_for_classes` can contain the same"
							+ " operation in both `allow_operations` and `deny_operations`.");
			System.exit(1);
		}
	}

	public boolean allowOperationsContains(OperationType operationType) {
		return this.allowOperationTypes.contains(operationType);
	}

	public Set<OperationType> getDenyOperationTypes() {
		return this.denyOperationTypes;
	}

	public void setDenyOperations(Set<String> deny_operations) {
		if (Collections.disjoint(deny_operations, this.allowOperations)) {
			this.denyOperations = deny_operations;
			this.denyOperationTypes =
					deny_operations.stream()
							.map(operation -> OperationType.valueOfLabel(operation))
							.collect(Collectors.toSet());
		} else {
			logger.severe(
					"In the YAML configuration file, no class in `paths_for_classes` can contain the same"
							+ " operation in both `allow_operations` and `deny_operations`.");
			System.exit(1);
		}
	}

	public boolean denyOperationsContains(OperationType operationType) {
		return this.denyOperationTypes.contains(operationType);
	}
}
