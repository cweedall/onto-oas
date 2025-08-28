package edu.isi.oba.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * Custom deserializer for {@link YamlConfig} that ensures post-processing is performed after
 * Jackson has populated the configuration object.
 *
 * <p>This deserializer invokes {@link YamlConfig#processConfig()} after deserialization to compute
 * derived configuration values and initialize internal structures.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @JsonDeserialize(using = YamlConfigDeserializer.class)
 * public class YamlConfig {
 *     ...
 * }
 * }</pre>
 */
public class YamlConfigDeserializer extends StdDeserializer<YamlConfig> {

	/** Constructs a new {@code YamlConfigDeserializer}. */
	public YamlConfigDeserializer() {
		super(YamlConfig.class);
	}

	/**
	 * Deserializes a {@link YamlConfig} object from YAML input and invokes {@link
	 * YamlConfig#processConfig()}.
	 *
	 * @param jp the JSON parser
	 * @param ctxt the deserialization context
	 * @return a fully initialized {@link YamlConfig} object
	 * @throws IOException if an I/O error occurs or {@link YamlConfig#processConfig()} fails
	 */
	@Override
	public YamlConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		ObjectMapper cleanMapper = new ObjectMapper(new YAMLFactory());
		cleanMapper.findAndRegisterModules(); // optional, if needed

		// Do NOT register the mix-in here
		YamlConfig config = cleanMapper.readValue(jp, YamlConfig.class);

		try {
			config.processConfig();
		} catch (OWLOntologyCreationException e) {
			throw new IOException("Ontology creation failed during config processing", e);
		}

		return config;
	}
}
