package edu.isi.oba.exceptions;

public class OntologyVisitorException extends RuntimeException {
	public OntologyVisitorException(String message) {
		super(message);
	}

	public OntologyVisitorException(String message, Throwable cause) {
		super(message, cause);
	}
}
