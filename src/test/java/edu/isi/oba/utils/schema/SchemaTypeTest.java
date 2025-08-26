package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.util.Date;
import org.junit.jupiter.api.Test;

class SchemaTypeTest extends BaseTest {

	@Test
	void testValueOfClassMappedTypes() {
		assertEquals(SchemaType.STRING, SchemaType.valueOfClass(String.class));
		assertEquals(SchemaType.INTEGER, SchemaType.valueOfClass(Integer.class));
		assertEquals(SchemaType.NUMBER, SchemaType.valueOfClass(Double.class));
		assertEquals(SchemaType.BOOLEAN, SchemaType.valueOfClass(Boolean.class));
		assertEquals(SchemaType.OBJECT, SchemaType.valueOfClass(Object.class));
	}

	@Test
	void testValueOfClassUnmappedType() {
		assertNull(SchemaType.valueOfClass(Date.class));
	}
}
