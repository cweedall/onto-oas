package edu.isi.oba.utils.json;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

	@Test
	public void testMergeJSONObjects() {
		JSONObject one = new JSONObject("{\"a\":1}");
		JSONObject two = new JSONObject("{\"b\":2}");
		JSONObject merged = JsonUtils.mergeJSONObjects(one, two);
		assertEquals(1, merged.getInt("a"));
		assertEquals(2, merged.getInt("b"));
	}

	@Test
	public void testConcatJSON() {
		JSONObject one = new JSONObject("{\"a\":1}");
		JSONObject two = new JSONObject("{\"b\":2}");
		JSONObject[] array = new JSONObject[] {one, two};
		JSONObject result = JsonUtils.concatJson(array);
		assertEquals(1, result.getInt("a"));
		assertEquals(2, result.getInt("b"));
	}

	@Test
	public void testConcatJSONCommonKey() {
		JSONObject one = new JSONObject("{\"common\":{\"a\":1}}");
		JSONObject two = new JSONObject("{\"common\":{\"b\":2}}");
		JSONObject[] array = new JSONObject[] {one, two};
		JSONObject result = JsonUtils.concatJsonCommonKey(array, "common");
		JSONObject common = result.getJSONObject("common");
		assertEquals(1, common.getInt("a"));
		assertEquals(2, common.getInt("b"));
	}

	@Test
	public void testReadJsonFile() throws Exception {
		JSONObject json = JsonUtils.readJsonFile("json_one.json");
		assertNotNull(json.get("@context"));
	}
}
