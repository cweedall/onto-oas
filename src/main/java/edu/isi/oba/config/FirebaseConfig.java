package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FirebaseConfig {
	public String key = "";

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
