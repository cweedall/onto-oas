package edu.isi.oba.utils.schema;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
	SchemaEnumUtilsTest.class,
	SchemaSortUtilsTest.class,
	SchemaCloneUtilsTest.class,
	SchemaRefUtilsTest.class
})
public class SchemaUtilsTestSuite {}
