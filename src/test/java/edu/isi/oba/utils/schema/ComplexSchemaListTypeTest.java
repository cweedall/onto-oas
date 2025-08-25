package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import org.junit.jupiter.api.Test;

class ComplexSchemaListTypeTest extends BaseTest {

	@Test
	void testEnumValues() {
		for (ComplexSchemaListType type : ComplexSchemaListType.values()) {
			assertNotNull(type.name());
		}
	}

	@Test
	void testValueOf() {
		assertEquals(ComplexSchemaListType.ALLOF_LIST, ComplexSchemaListType.valueOf("ALLOF_LIST"));
		assertEquals(ComplexSchemaListType.ANYOF_LIST, ComplexSchemaListType.valueOf("ANYOF_LIST"));
		assertEquals(ComplexSchemaListType.ONEOF_LIST, ComplexSchemaListType.valueOf("ONEOF_LIST"));
	}
}
