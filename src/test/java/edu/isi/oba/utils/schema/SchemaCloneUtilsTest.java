package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SchemaCloneUtilsTest extends BaseTest {

	@Test
	public void testCloneSimpleSchema() {
		Schema<String> original = new Schema<>();
		original.setType("string");
		original.setTitle("Original Title");
		original.setDescription("Original Description");

		Schema<?> cloned = SchemaCloneUtils.clone(original);

		assertNotNull(cloned);
		assertEquals("string", cloned.getType());
		assertEquals("Original Title", cloned.getTitle());
		assertEquals("Original Description", cloned.getDescription());
	}

	@Test
	public void testCloneSchemaWithEnum() {
		Schema<String> original = new Schema<>();
		original.setType("string");
		// original.setEnum(Arrays.asList());
		original.setEnum(Arrays.asList("A", "B", "C"));
		// original.addEnumItemObject((? extends Object) "A");

		Schema<?> cloned = new Schema<>();

		SchemaCloneUtils.getClonedSchemaWithDeepCopiedEnumValues(original, cloned);

		assertNotNull(cloned);
		assertEquals("string", cloned.getType());
		assertEquals(Arrays.asList("A", "B", "C"), cloned.getEnum());
	}

	@Test
	public void testDeepCopyListTypeValuesAllOf() {
		Schema<?> source = new Schema<>();
		List<Schema> allOfList = new ArrayList<>();
		allOfList.add(new Schema<>().type("string").title("A"));
		allOfList.add(new Schema<>().type("integer").title("B"));
		source.setAllOf(allOfList);

		Schema<?> target = new Schema<>();
		SchemaCloneUtils.deepCopyListTypeValues(source, target, ComplexSchemaListType.ALLOF_LIST);

		assertNotNull(target.getAllOf());
		assertEquals(2, target.getAllOf().size());
		assertEquals("A", target.getAllOf().get(0).getTitle());
		assertEquals("B", target.getAllOf().get(1).getTitle());
	}

	@Test
	public void testDeepCopyListTypeValuesAnyOf() {
		Schema<?> source = new Schema<>();
		List<Schema> anyOfList = new ArrayList<>();
		anyOfList.add(new Schema<>().type("boolean").title("X"));
		anyOfList.add(new Schema<>().type("number").title("Y"));
		source.setAnyOf(anyOfList);

		Schema<?> target = new Schema<>();
		SchemaCloneUtils.deepCopyListTypeValues(source, target, ComplexSchemaListType.ANYOF_LIST);

		assertNotNull(target.getAnyOf());
		assertEquals(2, target.getAnyOf().size());
		assertEquals("X", target.getAnyOf().get(0).getTitle());
		assertEquals("Y", target.getAnyOf().get(1).getTitle());
	}

	@Test
	public void testDeepCopyListTypeValuesOneOf() {
		Schema<?> source = new Schema<>();
		List<Schema> oneOfList = new ArrayList<>();
		oneOfList.add(new Schema<>().type("object").title("Obj1"));
		oneOfList.add(new Schema<>().type("array").title("Obj2"));
		source.setOneOf(oneOfList);

		Schema<?> target = new Schema<>();
		SchemaCloneUtils.deepCopyListTypeValues(source, target, ComplexSchemaListType.ONEOF_LIST);

		assertNotNull(target.getOneOf());
		assertEquals(2, target.getOneOf().size());
		assertEquals("Obj1", target.getOneOf().get(0).getTitle());
		assertEquals("Obj2", target.getOneOf().get(1).getTitle());
	}
}
