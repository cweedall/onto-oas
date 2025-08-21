package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

public class SchemaSortUtilsTest {

	@Test
	public void testTopologicalSort() {
		Map<String, Set<String>> graph = new HashMap<>();
		graph.put("A", Set.of("B"));
		graph.put("B", Set.of("C"));
		graph.put("C", Set.of());

		List<String> sorted = SchemaSortUtils.topologicalSort(graph);
		assertEquals(List.of("C", "B", "A"), sorted);
	}

	@Test
	public void testCycleDetection() {
		Map<String, Set<String>> graph = new HashMap<>();
		graph.put("A", Set.of("B"));
		graph.put("B", Set.of("A"));

		Exception exception =
				assertThrows(IllegalStateException.class, () -> SchemaSortUtils.topologicalSort(graph));

		assertTrue(exception.getMessage().contains("Cycle detected"));
	}
}
