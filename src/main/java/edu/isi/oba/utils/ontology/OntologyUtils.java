package edu.isi.oba.utils.ontology;

import edu.isi.oba.utils.constants.ObaConstants;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;

public class OntologyUtils {

	private static final Logger logger = Logger.getLogger(OntologyUtils.class.getName());

	/**
	 * Method that will download an ontology given its URI, doing content negotiation The ontology
	 * will be downloaded in the first serialization available (see
	 * Constants.POSSIBLE_VOCAB_SERIALIZATIONS)
	 *
	 * @param uri the URI of the ontology
	 * @param downloadPath path where the ontology will be saved locally.
	 */
	public static void downloadOntology(String uri, String downloadPath) {
		for (String serialization : ObaConstants.POSSIBLE_VOCAB_SERIALIZATIONS) {
			System.out.println("Attempting to download vocabulary in " + serialization);
			// logger is not initialized correctly and fails in tests (to fix)
			// logger.info("Attempting to download vocabulary in " + serialization);
			try {
				URL url = new URL(uri);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setInstanceFollowRedirects(true);
				connection.setRequestProperty("Accept", serialization);
				int status = connection.getResponseCode();
				boolean redirect = false;
				if (status != HttpURLConnection.HTTP_OK) {
					if (status == HttpURLConnection.HTTP_MOVED_TEMP
							|| status == HttpURLConnection.HTTP_MOVED_PERM
							|| status == HttpURLConnection.HTTP_SEE_OTHER) redirect = true;
				}
				// there are some vocabularies with multiple redirections:
				// 301 -> 303 -> owl
				while (redirect) {
					String newUrl = connection.getHeaderField("Location");
					connection = (HttpURLConnection) new URL(newUrl).openConnection();
					connection.setRequestProperty("Accept", serialization);
					status = connection.getResponseCode();
					if (status != HttpURLConnection.HTTP_MOVED_TEMP
							&& status != HttpURLConnection.HTTP_MOVED_PERM
							&& status != HttpURLConnection.HTTP_SEE_OTHER) redirect = false;
				}
				InputStream in = (InputStream) connection.getInputStream();
				Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
				in.close();
				break; // if the vocabulary is downloaded, then we don't download it for the other
				// serializations
			} catch (Exception e) {
				final String message =
						"Failed to download vocabulary in RDF format [" + serialization + "]: ";
				logger.severe(message + e.toString());
				throw new RuntimeException(message, e);
			}
		}
	}

	/**
	 * Method that given a class, property or data property, searches for the best description.
	 *
	 * @param entity entity to search.
	 * @param ontology ontology to be used to search descriptions.
	 * @param hasDefaultDescriptions flag indicating whether default descriptions should or should not
	 *     be included.
	 * @return Description string
	 */
	public static String getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions, String languageTag) {
		// Default to English (i.e. "en"), if null or empty/blank string
		if (languageTag == null || languageTag.isBlank()) {
			languageTag = "en";
		}

		long descriptionCount = 0;
		if (entity instanceof OWLObjectProperty) {
			for (final var description : ObaConstants.DESCRIPTION_PROPERTIES) {
				descriptionCount +=
						EntitySearcher.getAnnotationObjects(
										entity,
										Set.of(ontology).stream(),
										new OWLAnnotationPropertyImpl(new IRI(description) {}))
								.count();
			}

			if (descriptionCount == 0) {
				for (final var objPropRange :
						ontology.getObjectPropertyRangeAxioms(
								((OWLObjectProperty) entity).asObjectPropertyExpression())) {
					if (objPropRange.getRange() instanceof OWLClass) {
						entity = objPropRange.getRange().asOWLClass();
					}
				}
			}
		}

		// Use a map to keep track of all the description annotations.
		final var langDescMap = new HashMap<String, String>();

		for (final var description : ObaConstants.DESCRIPTION_PROPERTIES) {
			final var annotationObjectsStream =
					EntitySearcher.getAnnotationObjects(
							entity,
							Set.of(ontology).stream(),
							new OWLAnnotationPropertyImpl(new IRI(description) {}));
			final var annotationObjects = annotationObjectsStream.collect(Collectors.toSet());

			for (final var annotationObj : annotationObjects) {
				final var optionalDescriptionLiteral =
						((OWLAnnotation) annotationObj).getValue().asLiteral();

				if (optionalDescriptionLiteral.isPresent()) {
					final var descriptionLiteral = optionalDescriptionLiteral.get();
					final var yamlStringLineSep = "\n";
					final var specialTempChar = "\u001A";
					final var descLiteralValue =
							descriptionLiteral
									.getLiteral()
									.replaceAll("\r\n", specialTempChar)
									.replaceAll("\r", specialTempChar)
									.replaceAll("\n", specialTempChar)
									.replaceAll(specialTempChar, yamlStringLineSep);

					// A description is present, but if it has no language tag, use empty string "".
					// If there are multiple descriptions annotations with no language tag, only the last one
					// encountered will be used.
					langDescMap.put(
							descriptionLiteral.getLang() == null ? "" : descriptionLiteral.getLang(),
							descLiteralValue);
				}
			}
		}

		// Return description for the specified language tag, if it exists.  Otherwise, return
		// description with no language tag, if it exists.
		if (langDescMap.containsKey(languageTag)) {
			return langDescMap.get(languageTag);
		} else if (langDescMap.containsKey("")) {
			return langDescMap.get("");
		}

		// If no description was found, then return default description (if configured to do so) or
		// null, otherwise.
		return !Optional.ofNullable(hasDefaultDescriptions).orElse(false)
				? null
				: ObaConstants.DEFAULT_DESCRIPTION;
	}

	/**
	 * Convenience overloaded method for getDescription() that searches for the best English
	 * description of class, property, or data property.
	 *
	 * @param entity entity to search.
	 * @param ontology ontology to be used to search descriptions.
	 * @param hasDefaultDescriptions flag indicating whether default descriptions should or should not
	 *     be included.
	 * @return A string of the entity's English description.
	 */
	public static String getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions) {
		return getDescription(entity, ontology, hasDefaultDescriptions, "en");
	}
}
