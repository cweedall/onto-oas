package edu.isi.oba.utils.json;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Unit tests for JsonUtils class to ensure full coverage. */
public class JsonUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<JsonUtils> constructor = JsonUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	void shouldMergeFlatJsonObjects_whenKeysOverlap() {
		JSONObject json1 = new JSONObject("{\"a\": 1, \"b\": 2}");
		JSONObject json2 = new JSONObject("{\"b\": 3, \"c\": 4}");

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		assertEquals(1, result.getInt("a"));
		assertEquals(3, result.getInt("b"));
		assertEquals(4, result.getInt("c"));
	}

	@Test
	void shouldMergeNestedJsonObjects_whenValuesAreJsonObjects() {
		JSONObject json1 = new JSONObject("{\"a\": {\"x\": 1}}");
		JSONObject json2 = new JSONObject("{\"a\": {\"y\": 2}}");

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);
		JSONObject nested = result.getJSONObject("a");

		assertEquals(1, nested.getInt("x"));
		assertEquals(2, nested.getInt("y"));
	}

	@Test
	void shouldMergeAllJsonObjects_whenArrayIsValid() {
		JSONObject[] objects =
				new JSONObject[] {
					new JSONObject("{\"a\": 1}"), new JSONObject("{\"b\": 2}"), new JSONObject("{\"c\": 3}")
				};

		JSONObject result = JsonUtils.mergeAll(objects);

		assertEquals(1, result.getInt("a"));
		assertEquals(2, result.getInt("b"));
		assertEquals(3, result.getInt("c"));
	}

	@Test
	void shouldThrowException_whenMergeAllReceivesNull() {
		assertThrows(IllegalArgumentException.class, () -> JsonUtils.mergeAll(null));
	}

	@Test
	void shouldThrowException_whenMergeAllReceivesEmptyArray() {
		assertThrows(IllegalArgumentException.class, () -> JsonUtils.mergeAll(new JSONObject[] {}));
	}

	@Test
	void shouldMergeJsonObjectsByCommonKey_whenKeyExists() {
		JSONObject[] objects =
				new JSONObject[] {
					new JSONObject("{\"common\": {\"a\": 1}}"),
					new JSONObject("{\"common\": {\"b\": 2}}"),
					new JSONObject("{\"common\": {\"c\": 3}}")
				};

		JSONObject result = JsonUtils.mergeByCommonKey(objects, "common");
		JSONObject merged = result.getJSONObject("common");

		assertEquals(1, merged.getInt("a"));
		assertEquals(2, merged.getInt("b"));
		assertEquals(3, merged.getInt("c"));
	}

	@Test
	void shouldThrowException_whenMergeByCommonKeyReceivesNull() {
		assertThrows(IllegalArgumentException.class, () -> JsonUtils.mergeByCommonKey(null, "key"));
	}

	@Test
	void shouldThrowException_whenMergeByCommonKeyReceivesEmptyArray() {
		assertThrows(
				IllegalArgumentException.class,
				() -> JsonUtils.mergeByCommonKey(new JSONObject[] {}, "key"));
	}

	@Test
	void shouldThrowException_whenMergeByCommonKeyMissingKeyInFirstObject() {
		JSONObject[] objects =
				new JSONObject[] {
					new JSONObject("{\"other\": {\"a\": 1}}"), new JSONObject("{\"common\": {\"b\": 2}}")
				};

		assertThrows(JSONException.class, () -> JsonUtils.mergeByCommonKey(objects, "common"));
	}

	@Test
	void shouldReturnJsonObject_whenStreamContainsValidJson() {
		String jsonStr = "{\"key\": \"value\"}";
		InputStream stream = new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8));

		Optional<JSONObject> result = JsonUtils.readJsonFromStream(stream);

		assertTrue(result.isPresent());
		assertEquals("value", result.get().getString("key"));
	}

	@Test
	void shouldReturnEmptyOptional_whenStreamContainsInvalidJson() {
		String invalidJson = "{key: value"; // malformed
		InputStream stream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

		Optional<JSONObject> result = JsonUtils.readJsonFromStream(stream);

		assertFalse(result.isPresent());
	}

	@Test
	void shouldReturnEmptyOptional_whenStreamThrowsIOException() {
		InputStream stream =
				new InputStream() {
					@Override
					public int read() throws IOException {
						throw new IOException("Simulated read error");
					}
				};

		Optional<JSONObject> result = JsonUtils.readJsonFromStream(stream);

		assertFalse(result.isPresent());
	}

	@Test
	void shouldThrowIOException_whenReadJsonFileResourceNotFound() {
		IOException exception =
				assertThrows(IOException.class, () -> JsonUtils.readJsonFile("nonexistent.json"));
		assertTrue(exception.getMessage().contains("Resource not found"));
	}

	@Test
	void shouldMergeObjects_whenNextIsNotNull() {
		JSONObject obj1 = new JSONObject().put("data", new JSONObject().put("a", 1));
		JSONObject obj2 = new JSONObject().put("data", new JSONObject().put("b", 2));

		JSONObject[] input = new JSONObject[] {obj1, obj2};

		JSONObject result = JsonUtils.mergeByCommonKey(input, "data");

		JSONObject expected = new JSONObject().put("data", new JSONObject().put("a", 1).put("b", 2));
		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldSkipMerge_whenNextIsNull() {
		JSONObject obj1 = new JSONObject().put("data", new JSONObject().put("a", 1));
		JSONObject obj2 = new JSONObject(); // no "data" key

		JSONObject[] input = new JSONObject[] {obj1, obj2};

		JSONObject result = JsonUtils.mergeByCommonKey(input, "data");

		JSONObject expected = new JSONObject().put("data", new JSONObject().put("a", 1));
		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldThrowException_whenInputIsNullOrEmpty() {
		assertThrows(IllegalArgumentException.class, () -> JsonUtils.mergeByCommonKey(null, "data"));
		assertThrows(
				IllegalArgumentException.class,
				() -> JsonUtils.mergeByCommonKey(new JSONObject[0], "data"));
	}

	@Test
	void shouldThrowException_whenFirstObjectMissingCommonKey() {
		JSONObject obj1 = new JSONObject(); // no "data" key
		JSONObject obj2 = new JSONObject().put("data", new JSONObject().put("b", 2));

		JSONObject[] input = new JSONObject[] {obj1, obj2};

		assertThrows(org.json.JSONException.class, () -> JsonUtils.mergeByCommonKey(input, "data"));
	}

	@Test
	void shouldReturnCopy_whenJson2HasNoKeys() {
		JSONObject json1 = new JSONObject().put("a", 1);
		JSONObject json2 = new JSONObject(); // no keys

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		assertEquals(json1.toString(), result.toString());
	}

	@Test
	void shouldMerge_whenBothValuesAreJSONObjects() {
		JSONObject json1 = new JSONObject().put("nested", new JSONObject().put("a", 1));
		JSONObject json2 = new JSONObject().put("nested", new JSONObject().put("b", 2));

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		JSONObject expectedNested = new JSONObject().put("a", 1).put("b", 2);
		JSONObject expected = new JSONObject().put("nested", expectedNested);

		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldOverwrite_whenValue1IsJSONObjectAndValue2IsPrimitive() {
		JSONObject json1 = new JSONObject().put("key", new JSONObject().put("a", 1));
		JSONObject json2 = new JSONObject().put("key", "primitive");

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		JSONObject expected = new JSONObject().put("key", "primitive");
		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldOverwrite_whenValue1IsPrimitiveAndValue2IsJSONObject() {
		JSONObject json1 = new JSONObject().put("key", "primitive");
		JSONObject json2 = new JSONObject().put("key", new JSONObject().put("b", 2));

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		JSONObject expected = new JSONObject().put("key", new JSONObject().put("b", 2));
		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldOverwrite_whenBothValuesArePrimitive() {
		JSONObject json1 = new JSONObject().put("key", "old");
		JSONObject json2 = new JSONObject().put("key", "new");

		JSONObject result = JsonUtils.mergeJSONObjects(json1, json2);

		JSONObject expected = new JSONObject().put("key", "new");
		assertEquals(expected.toString(), result.toString());
	}

	@Test
	void shouldReturnJsonObject_whenReadJsonFileSucceeds() throws IOException {
		// Assumes test-resource.json is placed in src/test/resources
		JSONObject result = JsonUtils.readJsonFile("test.json");
		assertEquals("fileValue", result.getString("fileKey"));
	}

	@Test
	void shouldThrowIOException_whenReadJsonFileFailsToParse() {
		IOException exception =
				assertThrows(IOException.class, () -> JsonUtils.readJsonFile("malformed_test.json"));
		assertEquals("Failed to parse JSON from stream", exception.getMessage());
	}
}
