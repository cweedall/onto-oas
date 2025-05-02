package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationConfig {
	public PropertyAnnotationConfig property_annotations;
	private String markdown_generation_filename;
	public Set<MarkdownAnnotationConfig> markdown_generation_annotations;

	/**
	 * The property annotations may be null (because it doesn't exist in the config file). We wrap it
	 * within an {@link Optional} for determining whether a value exists.
	 *
	 * @return a {@link PropertyAnnotationConfig} parameterized {@link Optional}
	 */
	public Optional<PropertyAnnotationConfig> getProperty_annotations() {
		if (this.property_annotations != null) {
			return Optional.ofNullable(this.property_annotations);
		} else {
			return Optional.empty();
		}
	}

	public void setProperty_annotations(PropertyAnnotationConfig property_annotations) {
		this.property_annotations = property_annotations;
	}

	public String getMarkdown_generation_filename() {
		return this.markdown_generation_filename;
	}

	public void setMarkdown_generation_filename(String markdown_generation_filename) {
		this.markdown_generation_filename = markdown_generation_filename;
	}

	/**
	 * The README annotations may be null (because it doesn't exist in the config file). We wrap it
	 * within an {@link Optional} for determining whether a value exists.
	 *
	 * @return a {@link MarkdownAnnotationConfig} parameterized {@link Optional}
	 */
	public Optional<Set<MarkdownAnnotationConfig>> getMarkdown_generation_annotations() {
		if (this.markdown_generation_annotations != null) {
			return Optional.ofNullable(this.markdown_generation_annotations);
		} else {
			return Optional.empty();
		}
	}

	public void setMarkdown_generation_annotations(
			Set<MarkdownAnnotationConfig> readme_generation_annotations) {
		this.markdown_generation_annotations = readme_generation_annotations;
	}
}
