package edu.isi.oba.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Mix-in class for {@link YamlConfig} to apply a custom Jackson deserializer.
 *
 * <p>This mix-in is used to associate {@link YamlConfigDeserializer} with {@link YamlConfig}
 * without modifying the original {@code YamlConfig} class directly. This approach avoids recursive
 * deserialization issues that can occur when the deserializer internally calls {@code
 * ObjectMapper.readValue(..., YamlConfig.class)}.
 *
 * <p>Usage:
 *
 * <pre>
 *     ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
 *     mapper.addMixIn(YamlConfig.class, YamlConfigMixin.class);
 * </pre>
 *
 * This ensures that {@link YamlConfigDeserializer} is used during deserialization, and allows
 * {@code processConfig()} to be invoked automatically after the object is populated.
 *
 * @author Copilot
 */
@JsonDeserialize(using = YamlConfigDeserializer.class)
public abstract class YamlConfigMixin {
	// No implementation needed; this class only serves as a mix-in for Jackson.
}
