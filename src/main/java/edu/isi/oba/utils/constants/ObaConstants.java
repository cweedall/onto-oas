package edu.isi.oba.utils.constants;

import java.util.ArrayList;
import java.util.List;

public final class ObaConstants {
	public static final String DEFAULT_LANGUAGE = "en";
	public static final String YAML_LINE_SEPARATOR = "\n";

	public static final String DEFAULT_DESCRIPTION = "Description not available";
	public static final List<String> POSSIBLE_VOCAB_SERIALIZATIONS =
			List.of("application/rdf+xml", "text/turtle", "text/n3", "application/ld+json");

	private static final String DCELEMENTS_NS = "http://purl.org/dc/elements/1.1/";
	private static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	private static final String PROV_NS = "http://www.w3.org/ns/prov#";
	private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final String SKOS_NS = "http://www.w3.org/2004/02/skos/core#";

	public static final String DCELEMENTS_DEFINITION = DCELEMENTS_NS + "description";
	public static final String DCTERMS_DEFINITION = DCTERMS_NS + "description";
	public static final String RDFS_COMMENT = RDFS_NS + "comment";
	public static final String SKOS_DEFINITION = SKOS_NS + "definition";
	public static final String PROV_DEFINITION = PROV_NS + "definition";

	public static final List<String> DESCRIPTION_PROPERTIES =
			new ArrayList<>(
					List.of(
							DCELEMENTS_DEFINITION,
							DCTERMS_DEFINITION,
							RDFS_COMMENT,
							SKOS_DEFINITION,
							PROV_DEFINITION));

	private ObaConstants() {
		throw new UnsupportedOperationException("Static constants class");
	}
}
