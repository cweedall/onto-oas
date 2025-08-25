package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class SchemaFormatTypeTest extends BaseTest {

	@Test
	public void testMappedTypes() {
		assertEquals(SchemaFormatType.INT64, SchemaFormatType.valueOfClass(Long.class));
		assertEquals(SchemaFormatType.FLOAT, SchemaFormatType.valueOfClass(Float.class));
		assertEquals(SchemaFormatType.DOUBLE, SchemaFormatType.valueOfClass(Double.class));
		assertEquals(SchemaFormatType.DATE, SchemaFormatType.valueOfClass(LocalDate.class));
		assertEquals(SchemaFormatType.DATE_TIME, SchemaFormatType.valueOfClass(OffsetDateTime.class));
		assertEquals(SchemaFormatType.DATE_TIME, SchemaFormatType.valueOfClass(ZonedDateTime.class));
		assertEquals(SchemaFormatType.URI, SchemaFormatType.valueOfClass(URI.class));
		assertEquals(SchemaFormatType.IPV4, SchemaFormatType.valueOfClass(Inet4Address.class));
		assertEquals(SchemaFormatType.IPV6, SchemaFormatType.valueOfClass(Inet6Address.class));
	}

	@Test
	public void testUnmappedTypes() {
		assertNull(SchemaFormatType.valueOfClass(String.class)); // No format
		assertNull(SchemaFormatType.valueOfClass(Boolean.class)); // No format
		assertNull(SchemaFormatType.valueOfClass(Date.class)); // Not mapped
	}
}
