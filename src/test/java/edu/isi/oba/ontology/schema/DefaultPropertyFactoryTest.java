package edu.isi.oba.ontology.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultPropertyFactory}. */
public class DefaultPropertyFactoryTest extends BaseTest {

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<DefaultPropertyFactory> constructor =
				DefaultPropertyFactory.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	void shouldReturnDefaultProperties_whenConfigIsProvided() {
		Map<String, Schema> properties = DefaultPropertyFactory.getDefaultProperties();
		assertNotNull(properties);
		assertTrue(properties.containsKey("id"));
		assertTrue(properties.containsKey("label"));
		assertTrue(properties.containsKey("type"));
		assertTrue(properties.containsKey("description"));
		assertTrue(properties.containsKey("eventDateTime"));
		assertTrue(properties.containsKey("isBool"));
		assertTrue(properties.containsKey("quantity"));
	}
}
