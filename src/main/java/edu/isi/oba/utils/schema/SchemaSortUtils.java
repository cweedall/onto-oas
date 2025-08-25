package edu.isi.oba.utils.schema;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
				.log(Level.INFO, "Starting topological sort of schema dependencies...");
		Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
				.log(Level.FINE, "Initial dependency map: " + dependencyMap);

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

			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.FINE, "Processing schema: " + current);
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.FINE, "Remaining in-degree map: " + inDegree);

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
			Set<String> unresolved = new HashSet<>(dependencyMap.keySet());
			unresolved.removeAll(sortedSchemas);
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.SEVERE, "Cycle detected in schema references involving: " + unresolved);
			throw new IllegalStateException("Cycle detected in schema references: " + unresolved);
		}

		Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
				.log(Level.INFO, "Topological sort completed. Sorted schema order: " + sortedSchemas);

		return sortedSchemas;
	}

	/**
	 * Exports the schema dependency graph to a DOT file for visualization using tools like Graphviz.
	 * Each node in the graph represents a schema, and each directed edge indicates a dependency
	 * (i.e., one schema references another).
	 *
	 * <p>This is useful for debugging complex schema relationships or identifying cycles and deeply
	 * nested references.
	 *
	 * <pre>
	 * Example DOT output:
	 * digraph SchemaDependencies {
	 *   "Address" -> "Country";
	 *   "Order" -> "Customer";
	 * }
	 * </pre>
	 *
	 * @param dependencyMap a map where the key is a schema name and the value is a set of schema
	 *     names it depends on
	 * @param outputPath the file path where the DOT file will be written
	 * @throws IOException if writing to the file fails
	 */
	public static void exportDependencyGraph(
			Map<String, Set<String>> dependencyMap, Path outputPath) {
		try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
			writer.write("digraph SchemaDependencies {\n");
			for (var entry : dependencyMap.entrySet()) {
				for (var dep : entry.getValue()) {
					writer.write("  \"" + dep + "\" -> \"" + entry.getKey() + "\";\n");
				}
			}
			writer.write("}\n");
		} catch (IOException e) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.SEVERE, "Failed to export dependency graph", e);
		}
	}
}
