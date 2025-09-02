package edu.isi.oba.utils.json;

import edu.isi.oba.Oba;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {
	public static JSONObject concatJsonCommonKey(JSONObject[] objects, String common_key) {
		JSONObject mergeJSON = (JSONObject) objects[0].get(common_key);
		for (int i = 1; i < objects.length; i++) {
			mergeJSON = mergeJSONObjects(mergeJSON, (JSONObject) objects[i].get(common_key));
		}
		return new JSONObject().put(common_key, mergeJSON);
	}

	public static JSONObject concatJson(JSONObject[] objects) {
		JSONObject mergeJSON = objects[0];
		for (int i = 1; i < objects.length; i++) {
			mergeJSON = mergeJSONObjects(mergeJSON, objects[i]);
		}
		return mergeJSON;
	}

	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {

		JSONObject mergedJSON = new JSONObject();
		try {
			mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
			for (String crunchifyKey : JSONObject.getNames(json2)) {
				mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
			}

		} catch (JSONException e) {
			throw new RuntimeException("JSON Exception" + e);
		}
		return mergedJSON;
	}

	/**
	 * @param file_name
	 * @return
	 * @throws IOException
	 */
	public static JSONObject readJsonFile(String file_name) throws IOException {
		InputStream stream = Oba.class.getClassLoader().getResourceAsStream(file_name);
		byte b[] = new byte[stream.available()];
		JSONObject jsonObject = null;
		if (stream.read(b) == b.length) {
			jsonObject = new JSONObject(new String(b));
		}
		return jsonObject;
	}
}
