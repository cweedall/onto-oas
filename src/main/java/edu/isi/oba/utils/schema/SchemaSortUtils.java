package edu.isi.oba.utils.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class SchemaSortUtils {
	/**
	 * Topological sorting of the names of {@link Schema}s in a {@link Map} of dependencies (name of
	 * {@link Schema} to a {@link Set} of all the {@link Schema}s that it references).
	 *
	 * @param dependencyMap a {@link Map} of dependencies
	 * @return a {@link List} of the {@link Schema} names in order from no/least to most references to
	 *     other {@link Schema}s.
	 */
	public static List<String> topologicalSort(Map<String, Set<String>> dependencyMap) {
		final Map<String, Integer> inDegree = new HashMap<>();
		final Map<String, List<String>> graph = new HashMap<>();

		// Initialize in-degree and graph
		for (final var schema : dependencyMap.keySet()) {
			inDegree.put(schema, 0);
			graph.put(schema, new ArrayList<>());
		}

		// Build graph and compute in-degrees
		for (final var entry : dependencyMap.entrySet()) {
			String schema = entry.getKey();
			for (String dependency : entry.getValue()) {
				graph.computeIfAbsent(dependency, k -> new ArrayList<>()).add(schema);
				inDegree.put(schema, inDegree.getOrDefault(schema, 0) + 1);
			}
		}

		// Queue for schemas with no dependencies
		final Queue<String> queue = new LinkedList<>();
		for (final var entry : inDegree.entrySet()) {
			if (entry.getValue() == 0) {
				queue.add(entry.getKey());
			}
		}

		final List<String> sortedSchemas = new ArrayList<>();
		while (!queue.isEmpty()) {
			final var current = queue.poll();
			sortedSchemas.add(current);

			for (final var neighbor : graph.getOrDefault(current, Collections.emptyList())) {
				inDegree.put(neighbor, inDegree.get(neighbor) - 1);
				if (inDegree.get(neighbor) == 0) {
					queue.add(neighbor);
				}
			}
		}

		// Detect cycles
		if (sortedSchemas.size() != dependencyMap.size()) {
			throw new IllegalStateException("Cycle detected in schema references");
		}

		return sortedSchemas;
	}
}
