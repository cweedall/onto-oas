package edu.isi.oba.config.ontology.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationConfig {
	public PropertyAnnotationConfig property_annotations;
	private String markdown_generation_filename;
	public Set<MarkdownAnnotationConfig> markdown_generation_annotations;

	/**
	 * Get the {@link PropertyAnnotationConfig} which may be null (because it doesn't exist in the
	 * config file).
	 *
	 * @return a {@link PropertyAnnotationConfig}
	 */
	public PropertyAnnotationConfig getProperty_annotations() {
		return this.property_annotations;
	}

	public void setProperty_annotations(PropertyAnnotationConfig property_annotations) {
		if (property_annotations == null) {
			this.property_annotations = new PropertyAnnotationConfig();
		} else {
			this.property_annotations = property_annotations;
		}
	}

	public String getMarkdown_generation_filename() {
		return this.markdown_generation_filename;
	}

	public void setMarkdown_generation_filename(String markdown_generation_filename) {
		this.markdown_generation_filename = markdown_generation_filename;
	}

	/**
	 * Get the README annotations which may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link MarkdownAnnotationConfig}
	 */
	public Set<MarkdownAnnotationConfig> getMarkdown_generation_annotations() {
		return this.markdown_generation_annotations;
	}

	public void setMarkdown_generation_annotations(
			Set<MarkdownAnnotationConfig> readme_generation_annotations) {
		if (markdown_generation_annotations == null) {
			this.markdown_generation_annotations = new HashSet<MarkdownAnnotationConfig>();
		} else {
			this.markdown_generation_annotations = readme_generation_annotations;
		}
	}
}
