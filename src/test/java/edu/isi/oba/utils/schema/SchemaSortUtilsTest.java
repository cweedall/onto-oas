package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

public class SchemaSortUtilsTest extends BaseTest {

	@Test
	public void testTopologicalSortSimple() {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of());
		deps.put("B", Set.of("A"));
		deps.put("C", Set.of("B"));

		List<String> sorted = SchemaSortUtils.topologicalSort(deps);
		assertEquals(List.of("A", "B", "C"), sorted);
	}

	@Test
	public void testTopologicalSortCycleDetection() {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of("B"));
		deps.put("B", Set.of("A"));

		Exception exception =
				assertThrows(
						IllegalStateException.class,
						() -> {
							SchemaSortUtils.topologicalSort(deps);
						});

		assertTrue(exception.getMessage().contains("Cycle detected"));
	}

	@Test
	public void testExportDependencyGraph() throws Exception {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of("B"));
		deps.put("B", Set.of());

		Path tempFile = Files.createTempFile("schema_graph", ".dot");
		SchemaSortUtils.exportDependencyGraph(deps, tempFile);

		String content = Files.readString(tempFile);
		assertTrue(content.contains("digraph SchemaDependencies {"));
		assertTrue(content.contains("  \"B\" -> \"A\";"));
		assertTrue(content.contains("}"));

		Files.delete(tempFile);
	}
}
