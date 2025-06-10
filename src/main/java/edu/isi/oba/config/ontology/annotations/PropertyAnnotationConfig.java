package edu.isi.oba.config.ontology.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import edu.isi.oba.config.ConfigPropertyNames;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.PROPERTY_ANNOTATIONS)
public class PropertyAnnotationConfig {
	@JsonProperty(ConfigPropertyNames.READ_ONLY_FLAG_NAME)
	private String readOnlyFlagName;

	@JsonProperty(ConfigPropertyNames.WRITE_ONLY_FLAG_NAME)
	private String writeOnlyFlagName;

	@JsonProperty(ConfigPropertyNames.EXAMPLE_VALUE_NAME)
	private String exampleValueName;

	public String getReadOnlyFlagName() {
		return readOnlyFlagName;
	}

	public void setReadOnlyFlagName(String readOnlyFlagName) {
		this.readOnlyFlagName = readOnlyFlagName;
	}

	public String getWriteOnlyFlagName() {
		return writeOnlyFlagName;
	}

	public void setWriteOnlyFlagName(String writeOnlyFlagName) {
		this.writeOnlyFlagName = writeOnlyFlagName;
	}

	public String getExampleValueName() {
		return exampleValueName;
	}

	public void setExampleValueName(String exampleValueName) {
		this.exampleValueName = exampleValueName;
	}
}
