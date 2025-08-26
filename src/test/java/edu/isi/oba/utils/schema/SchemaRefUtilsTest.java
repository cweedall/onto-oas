package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class SchemaRefUtilsTest extends BaseTest {

	@Test
	public void testGetDereferencedSchemasParallel() {
		// Create the referenced schema
		Schema<?> addressSchema = new Schema<>();
		addressSchema.setType("object");
		addressSchema.setTitle("Address");
		addressSchema.setDescription("Address schema");

		// Create the referencing schema with a $ref to Address
		Schema<?> userSchema = new Schema<>();
		userSchema.setType("object");
		userSchema.setTitle("User");
		userSchema.setDescription("User schema");

		Schema<?> addressRef = new Schema<>();
		addressRef.set$ref("#/components/schemas/Address");

		// Use setProperties instead of deprecated addProperties
		@SuppressWarnings("rawtypes")
		Map<String, Schema> properties = new HashMap<>();
		properties.put("address", addressRef);
		userSchema.setProperties(properties);

		// Create schema map with correct type
		@SuppressWarnings("rawtypes")
		Map<String, Schema> schemaMap = new HashMap<>();
		schemaMap.put("Address", addressSchema);
		schemaMap.put("User", userSchema);

		// Call the method under test
		@SuppressWarnings("rawtypes")
		Map<String, Schema> dereferenced = SchemaRefUtils.getDereferencedSchemasParallel(schemaMap);

		// Assertions
		assertEquals(2, dereferenced.size());
		assertTrue(dereferenced.containsKey("User"));
		assertTrue(dereferenced.containsKey("Address"));

		Schema<?> dereferencedUser = dereferenced.get("User");
		assertNotNull(dereferencedUser.getProperties());
		Schema<?> dereferencedAddress = dereferencedUser.getProperties().get("address");

		assertEquals("Address", dereferencedAddress.getTitle());
		assertEquals("Address schema", dereferencedAddress.getDescription());
		assertEquals("object", dereferencedAddress.getType());
	}
}
