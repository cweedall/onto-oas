package edu.isi.oba.config.paths;

import static edu.isi.oba.Oba.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import edu.isi.oba.config.ConfigPropertyNames;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.IRI;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
	ConfigPropertyNames.CLASS_IRI,
	ConfigPropertyNames.ALLOW_OPERATIONS,
	ConfigPropertyNames.DENY_OPERATIONS
})
@JsonRootName(ConfigPropertyNames.PATHS_FOR_CLASSES)
public class PathsForClassConfig {
	private IRI classIRI;
	private Set<OperationType> allowOperationTypes = new HashSet<>();
	private Set<String> allowOperations = new HashSet<>();
	private Set<OperationType> denyOperationTypes = new HashSet<>();
	private Set<String> denyOperations = new HashSet<>();

	public IRI getClassIRI() {
		return this.classIRI;
	}

	@JsonSetter(ConfigPropertyNames.CLASS_IRI)
	private void setClassIri(String classIriString) {
		this.classIRI = IRI.create(classIriString);
	}

	public Set<OperationType> getAllowOperationTypes() {
		return this.allowOperationTypes;
	}

	@JsonSetter(value = ConfigPropertyNames.ALLOW_OPERATIONS, nulls = Nulls.AS_EMPTY)
	private void setAllowOperations(Set<String> allowOperations) {
		if (Collections.disjoint(allowOperations, this.denyOperations)) {
			this.allowOperations = allowOperations;
			this.allowOperationTypes =
					allowOperations.stream()
							.map(operation -> OperationType.valueOfLabel(operation))
							.collect(Collectors.toSet());
		} else {
			logger.severe(
					"YAML config error:: Classes in `paths_for_classes` cannot contain the same"
							+ " entry in `allow_operations` and `deny_operations`.  See class IRI: '"
							+ this.classIRI
							+ "'");
			System.exit(1);
		}
	}

	public boolean allowOperationsContains(OperationType operationType) {
		return this.allowOperationTypes.contains(operationType);
	}

	public Set<OperationType> getDenyOperationTypes() {
		return this.denyOperationTypes;
	}

	@JsonSetter(value = ConfigPropertyNames.DENY_OPERATIONS, nulls = Nulls.AS_EMPTY)
	private void setDenyOperations(Set<String> denyOperations) {
		if (Collections.disjoint(denyOperations, this.allowOperations)) {
			this.denyOperations = denyOperations;
			this.denyOperationTypes =
					denyOperations.stream()
							.map(operation -> OperationType.valueOfLabel(operation))
							.collect(Collectors.toSet());
		} else {
			logger.severe(
					"YAML config error:: Classes in `paths_for_classes` cannot contain the same"
							+ " entry in `allow_operations` and `deny_operations`.  See class IRI: '"
							+ this.classIRI
							+ "'");
			System.exit(1);
		}
	}

	public boolean denyOperationsContains(OperationType operationType) {
		return this.denyOperationTypes.contains(operationType);
	}
}
