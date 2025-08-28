package edu.isi.oba.exceptions;

public class OntologyLoadingException extends RuntimeException {
	public OntologyLoadingException(String message) {
		super(message);
	}

	public OntologyLoadingException(String message, Throwable cause) {
		super(message, cause);
	}
}
