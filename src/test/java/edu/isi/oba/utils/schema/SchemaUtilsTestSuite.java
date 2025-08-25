package edu.isi.oba.utils.schema;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
	ComplexSchemaListTypeTest.class,
	ComplexSchemaListUtilsTest.class,
	SchemaCloneUtilsTest.class,
	SchemaEnumUtilsTest.class,
	SchemaFormatTypeTest.class,
	SchemaRefUtilsTest.class,
	SchemaSortUtilsTest.class,
	SchemaTypeTest.class
})
public class SchemaUtilsTestSuite {}
