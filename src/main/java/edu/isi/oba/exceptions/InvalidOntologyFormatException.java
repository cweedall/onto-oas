package edu.isi.oba.exceptions;

/** Exception thrown when an ontology does not have a valid prefix document format. */
public class InvalidOntologyFormatException extends RuntimeException {
	public InvalidOntologyFormatException(String message) {
		super(message);
	}
}
