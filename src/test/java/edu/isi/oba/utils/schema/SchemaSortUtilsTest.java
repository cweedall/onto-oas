package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import edu.isi.oba.BaseTest;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SchemaSortUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<SchemaSortUtils> constructor = SchemaSortUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

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

	@Test
	public void shouldLogError_whenExportDependencyGraphFails() throws Exception {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of("B"));
		deps.put("B", Set.of());

		Path dummyPath = Path.of("dummy.dot");

		// Mock Files.newBufferedWriter to throw IOException
		try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
			filesMock
					.when(() -> Files.newBufferedWriter(dummyPath))
					.thenThrow(new IOException("Simulated failure"));

			// Should not throw, just log the error
			assertDoesNotThrow(() -> SchemaSortUtils.exportDependencyGraph(deps, dummyPath));
		}
	}

	@Test
	public void shouldHandleMultipleSchemasDependingOnSameDependency_whenSorting() {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of("X"));
		deps.put("B", Set.of("X"));
		deps.put("X", Set.of());

		List<String> sorted = SchemaSortUtils.topologicalSort(deps);
		assertEquals(3, sorted.size());
		assertTrue(sorted.indexOf("X") < sorted.indexOf("A"));
		assertTrue(sorted.indexOf("X") < sorted.indexOf("B"));
	}

	@Test
	public void shouldNotAddNeighborToQueue_whenInDegreeIsGreaterThanZero() {
		Map<String, Set<String>> deps = new HashMap<>();
		deps.put("A", Set.of("X"));
		deps.put("B", Set.of("X"));
		deps.put("X", Set.of());

		List<String> sorted = SchemaSortUtils.topologicalSort(deps);
		assertEquals(List.of("X", "A", "B"), sorted); // or any valid topological order
	}
}
