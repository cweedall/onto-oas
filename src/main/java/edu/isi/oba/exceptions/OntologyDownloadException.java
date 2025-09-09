package edu.isi.oba.exceptions;

import java.io.IOException;

public class OntologyDownloadException extends IOException {
	public OntologyDownloadException(String message, Throwable cause) {
		super(message, cause);
	}
}
