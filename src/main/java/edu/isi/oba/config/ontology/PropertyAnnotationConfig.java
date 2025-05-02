package edu.isi.oba.config.ontology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyAnnotationConfig {
	private String read_only_flag_name;
	private String write_only_flag_name;
	private String example_value_name;

	public String getRead_only_flag_name() {
		return read_only_flag_name;
	}

	public void setRead_only_flag_name(String read_only_flag_name) {
		this.read_only_flag_name = read_only_flag_name;
	}

	public String getWrite_only_flag_name() {
		return write_only_flag_name;
	}

	public void setWrite_only_flag_name(String write_only_flag_name) {
		this.write_only_flag_name = write_only_flag_name;
	}

	public String getExample_value_name() {
		return example_value_name;
	}

	public void setExample_value_name(String example_value_name) {
		this.example_value_name = example_value_name;
	}
}
