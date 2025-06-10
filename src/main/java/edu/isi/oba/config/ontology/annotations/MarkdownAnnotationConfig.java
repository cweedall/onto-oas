package edu.isi.oba.config.ontology.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import edu.isi.oba.config.ConfigPropertyNames;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.MARKDOWN_GENERATION_ANNOTATIONS)
public class MarkdownAnnotationConfig {
	@JsonProperty(ConfigPropertyNames.ANNOTATION_NAME)
	private String annotationName;

	@JsonProperty(ConfigPropertyNames.MARKDOWN_HEADING)
	private String markdownHeading;

	@JsonProperty(ConfigPropertyNames.MARKDOWN_DESCRIPTION)
	private String markdownDescription;

	public String getAnnotationName() {
		return annotationName;
	}

	public void setAnnotationName(String annotationName) {
		this.annotationName = annotationName;
	}

	public String getMarkdownHeading() {
		return markdownHeading;
	}

	public void setMarkdownHeading(String markdownHeading) {
		this.markdownHeading = markdownHeading;
	}

	public String getMarkdownDescription() {
		return markdownDescription;
	}

	public void setMarkdownDescription(String markdownDescription) {
		this.markdownDescription = markdownDescription;
	}
}
