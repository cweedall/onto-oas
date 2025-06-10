package edu.isi.oba.config.ontology.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import edu.isi.oba.config.ConfigPropertyNames;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.ANNOTATION_CONFIG)
public class AnnotationConfig {
	@JsonProperty(ConfigPropertyNames.PROPERTY_ANNOTATIONS)
	private PropertyAnnotationConfig propertyAnnotations;

	@JsonProperty(ConfigPropertyNames.MARKDOWN_GENERATION_FILENAME)
	private String markdownGenerationFilename;

	@JsonProperty(ConfigPropertyNames.MARKDOWN_GENERATION_ANNOTATIONS)
	private Set<MarkdownAnnotationConfig> markdownGenerationAnnotations =
			new HashSet<MarkdownAnnotationConfig>();

	/**
	 * Get the {@link PropertyAnnotationConfig} which may be null (because it doesn't exist in the
	 * config file).
	 *
	 * @return a {@link PropertyAnnotationConfig}
	 */
	public PropertyAnnotationConfig getPropertyAnnotations() {
		return this.propertyAnnotations;
	}

	public void setPropertyAnnotations(PropertyAnnotationConfig propertyAnnotations) {
		if (propertyAnnotations == null) {
			this.propertyAnnotations = new PropertyAnnotationConfig();
		} else {
			this.propertyAnnotations = propertyAnnotations;
		}
	}

	public String getMarkdownGenerationFilename() {
		return this.markdownGenerationFilename;
	}

	public void setMarkdownGenerationFilename(String markdownGenerationFilename) {
		this.markdownGenerationFilename = markdownGenerationFilename;
	}

	/**
	 * Get the README annotations which may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link MarkdownAnnotationConfig}
	 */
	public Set<MarkdownAnnotationConfig> getMarkdownGenerationAnnotations() {
		return this.markdownGenerationAnnotations;
	}

	public void setMarkdownGenerationAnnotations(
			Set<MarkdownAnnotationConfig> readmeGenerationAnnotations) {
		this.markdownGenerationAnnotations = readmeGenerationAnnotations;
	}
}
