package edu.isi.oba.utils.json;

import edu.isi.oba.Oba;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for common JSON operations such as merging and reading JSON files. This class is
 * designed to be used statically and cannot be instantiated.
 */
public final class JsonUtils {

	private static final Logger LOGGER = Logger.getLogger(JsonUtils.class.getName());

	private JsonUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Merges multiple JSON objects under a shared key. Each object must contain a JSONObject under
	 * the specified key. The values under the key are recursively merged.
	 *
	 * @param objects an array of JSONObjects to merge
	 * @param commonKey the key under which the JSONObjects are nested
	 * @return a new JSONObject containing the merged result under the common key
	 * @throws JSONException if the common key is missing in the first object
	 */
	public static JSONObject mergeByCommonKey(JSONObject[] objects, String commonKey) {
		if (objects == null || objects.length == 0) {
			throw new IllegalArgumentException("Input JSON array is empty or null");
		}

		JSONObject merged = objects[0].optJSONObject(commonKey);
		if (merged == null) {
			throw new JSONException("Missing common key in first object: " + commonKey);
		}

		for (int i = 1; i < objects.length; i++) {
			JSONObject next = objects[i].optJSONObject(commonKey);
			if (next != null) {
				merged = mergeJSONObjects(merged, next);
			}
		}

		return new JSONObject().put(commonKey, merged);
	}

	/**
	 * Merges all JSONObjects in the array into a single JSONObject. If keys overlap, later values
	 * overwrite earlier ones unless both are JSONObjects, in which case they are recursively merged.
	 *
	 * @param objects an array of JSONObjects to merge
	 * @return a new JSONObject containing the merged result
	 * @throws IllegalArgumentException if the input array is null or empty
	 */
	public static JSONObject mergeAll(JSONObject[] objects) {
		if (objects == null || objects.length == 0) {
			throw new IllegalArgumentException("Input JSON array is empty or null");
		}

		JSONObject merged = new JSONObject(objects[0].toString());
		for (int i = 1; i < objects.length; i++) {
			merged = mergeJSONObjects(merged, objects[i]);
		}
		return merged;
	}

	/**
	 * Recursively merges two JSONObjects. If both values for a key are JSONObjects, they are merged
	 * recursively. Otherwise, the value from {@code json2} overwrites the value from {@code json1}.
	 *
	 * @param json1 the base JSONObject
	 * @param json2 the JSONObject to merge into the base
	 * @return a new JSONObject containing the merged result
	 */
	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
		JSONObject merged = new JSONObject(json1.toString());

		String[] keys = JSONObject.getNames(json2);
		if (keys != null) {
			for (String key : keys) {
				Object value2 = json2.get(key);
				Object value1 = merged.opt(key);

				if (value1 instanceof JSONObject && value2 instanceof JSONObject) {
					merged.put(key, mergeJSONObjects((JSONObject) value1, (JSONObject) value2));
				} else {
					merged.put(key, value2);
				}
			}
		}

		return merged;
	}

	/**
	 * Reads a JSON file from the classpath and parses it into a JSONObject.
	 *
	 * @param fileName the name of the file to read
	 * @return the parsed JSONObject
	 * @throws IOException if the file is not found or cannot be parsed
	 */
	public static JSONObject readJsonFile(String fileName) throws IOException {
		try (InputStream stream = getStream(fileName)) {
			if (stream == null) {
				throw new IOException("Resource not found: " + fileName);
			}
			Optional<JSONObject> result = readJsonFromStream(stream);
			return result.orElseThrow(() -> new IOException("Failed to parse JSON from stream"));
		}
	}

	static InputStream getStream(String fileName) {
		return Oba.class.getClassLoader().getResourceAsStream(fileName);
	}

	/**
	 * Reads JSON content from an InputStream and returns it as an Optional. If reading or parsing
	 * fails, returns {@code Optional.empty()}.
	 *
	 * @param stream the InputStream to read from
	 * @return an Optional containing the parsed JSONObject, or empty if an error occurs
	 */
	public static Optional<JSONObject> readJsonFromStream(InputStream stream) {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			byte[] data = new byte[1024];
			int nRead;
			while ((nRead = stream.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			return Optional.of(new JSONObject(buffer.toString(StandardCharsets.UTF_8)));
		} catch (IOException | JSONException e) {
			LOGGER.log(Level.SEVERE, "Error reading JSON from stream", e);
			return Optional.empty();
		}
	}
}
