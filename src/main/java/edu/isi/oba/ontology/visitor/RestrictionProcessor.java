package edu.isi.oba.ontology.visitor;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperObjectProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import edu.isi.oba.utils.constants.ObaConstants;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.*;

public class RestrictionProcessor {

	private RestrictionProcessor() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Processes quantified object restrictions such as someValuesFrom, allValuesFrom, and
	 * cardinalities.
	 *
	 * @param or the OWL quantified object restriction
	 */
	public static void processQuantifiedObjectRestriction(
			OWLQuantifiedObjectRestriction or,
			OWLObjectVisitor visitor,
			VisitorContext context,
			Logger logger) {
		Schema schema = getOrCreateSchema(visitor, context);
		OWLClassExpression filler = or.getFiller();

		if (filler instanceof OWLObjectOneOf) {
			filler.accept(visitor);
		} else if (filler instanceof OWLNaryBooleanClassExpression) {
			Schema composed =
					MapperObjectProperty.getComplexObjectComposedSchema(
							(OWLNaryBooleanClassExpression) filler);
			applyObjectRestriction(schema, or, composed, logger);
		} else {
			String range =
					SchemaBuilder.getPrefixedSchemaName(filler.asOWLClass(), context.getBaseClassOntology());
			applyObjectRestriction(schema, or, range, logger);
		}
	}

	/**
	 * Processes quantified data restrictions such as someValuesFrom, allValuesFrom, and
	 * cardinalities.
	 *
	 * @param dr the OWL quantified data restriction
	 */
	public static void processQuantifiedDataRestriction(
			OWLQuantifiedDataRestriction dr,
			OWLObjectVisitor visitor,
			VisitorContext context,
			Logger logger) {
		Schema schema = getOrCreateSchema(visitor, context);
		OWLDataRange filler = dr.getFiller();
		Integer cardinality =
				(dr instanceof OWLDataCardinalityRestriction)
						? ((OWLDataCardinalityRestriction) dr).getCardinality()
						: null;

		if (filler instanceof OWLDataOneOf) {
			filler.accept(visitor);
		} else if (filler instanceof OWLNaryDataRange) {
			Schema composed = MapperDataProperty.getComplexDataComposedSchema((OWLNaryDataRange) filler);
			applyDataRestriction(schema, dr, composed, cardinality, logger);
		} else if (filler instanceof OWLDatatypeRestriction) {
			OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) filler;
			String range = restriction.getDatatype().getIRI().getShortForm();
			for (OWLFacetRestriction facet : restriction.getFacetRestrictions()) {
				applyDataRestriction(schema, dr, range, cardinality, logger);
				MapperDataProperty.addDatatypeRestrictionToPropertySchema(schema, facet);
			}
		} else {
			String range = filler.asOWLDatatype().getIRI().getShortForm();
			applyDataRestriction(schema, dr, range, cardinality, logger);
		}
	}

	/**
	 * Processes n-ary boolean class expressions (e.g., unionOf, intersectionOf) for object
	 * properties.
	 *
	 * @param ce the OWL n-ary boolean class expression
	 */
	public static void processNaryBooleanClassExpression(
			OWLNaryBooleanClassExpression ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		schema.setItems(MapperObjectProperty.getComplexObjectComposedSchema(ce));
		MapperProperty.setSchemaType(schema, "array");
	}

	/**
	 * Processes n-ary data ranges (e.g., unionOf, intersectionOf) for data properties.
	 *
	 * @param ce the OWL n-ary data range
	 */
	public static void processNaryDataRange(
			OWLNaryDataRange ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		schema.setItems(MapperDataProperty.getComplexDataComposedSchema(ce));
		MapperProperty.setSchemaType(schema, "array");
	}

	/**
	 * Processes hasValue restrictions for object or data properties.
	 *
	 * @param ce the OWL hasValue restriction
	 */
	public static void processHasValue(
			OWLObjectHasValue ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		if (ce.getFiller() instanceof OWLNamedIndividual) {
			MapperObjectProperty.addHasValueOfPropertySchema(schema, ce.getFiller());
		}
	}

	/**
	 * Processes hasValue restrictions for object or data properties.
	 *
	 * @param ce the OWL hasValue restriction
	 */
	public static void processHasValue(
			OWLDataHasValue ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		if (ce.getFiller().getDatatype() != null) {
			MapperDataProperty.addHasValueOfPropertySchema(schema, ce.getFiller());
		}
	}

	/**
	 * Processes oneOf enumerations for object or data properties.
	 *
	 * @param ce the OWL oneOf restriction
	 */
	public static void processOneOf(
			OWLObjectOneOf ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		for (OWLIndividual individual : ce.getIndividuals()) {
			MapperObjectProperty.addOneOfToObjectPropertySchema(
					schema, individual.asOWLNamedIndividual().getIRI().getShortForm());
		}
	}

	/**
	 * Processes oneOf enumerations for object or data properties.
	 *
	 * @param ce the OWL oneOf restriction
	 */
	public static void processOneOf(
			OWLDataOneOf ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		ce.values().forEach(value -> MapperDataProperty.addOneOfDataPropertySchema(schema, value));
	}

	/**
	 * Processes complementOf restrictions for object or data properties.
	 *
	 * @param ce the OWL complementOf restriction
	 */
	public static void processComplementOf(
			OWLObjectComplementOf ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		String range =
				SchemaBuilder.getPrefixedSchemaName(
						ce.getOperand().asOWLClass(), context.getBaseClassOntology());
		MapperObjectProperty.setComplementOfForObjectSchema(schema, range);
	}

	/**
	 * Processes complementOf restrictions for object or data properties.
	 *
	 * @param ce the OWL complementOf restriction
	 */
	public static void processComplementOf(
			OWLDataComplementOf ce, OWLObjectVisitor visitor, VisitorContext context) {
		Schema schema = getOrCreateSchema(visitor, context);
		ce.datatypesInSignature()
				.forEach(dt -> MapperDataProperty.setComplementOfForDataSchema(schema, dt));
	}

	static Schema getOrCreateSchema(OWLObjectVisitor visitor, VisitorContext context) {
		final var propertyName = context.getCurrentlyProcessedPropertyName();
		Schema schema = null;

		if (propertyName == null) {
			// Dealing directly with the class schema.  This should always exist already.
			schema = context.getClassSchema();
		} else {
			schema =
					(Schema)
							Optional.ofNullable(context.getClassSchema().getProperties())
									.map(props -> props.get(propertyName))
									.orElse(null);

			// If schema is null, this is a new property that needs to have a schema created and added.
			if (schema == null) {
				schema = new ArraySchema();
				MapperProperty.setSchemaName(schema, propertyName);
				String description =
						GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS)
								? ObaConstants.DEFAULT_DESCRIPTION
								: null;
				MapperProperty.setSchemaDescription(schema, description);
				context.addPropertySchemaToClassSchema(propertyName, schema);
			}
		}

		return schema;
	}

	/**
	 * Applies object property restrictions to the schema based on the restriction type.
	 *
	 * @param schema the schema to update
	 * @param or the object restriction
	 * @param range the range (either a String or Schema) to apply
	 */
	static void applyObjectRestriction(
			Schema schema, OWLQuantifiedObjectRestriction or, Object range, Logger logger) {
		if (or instanceof OWLObjectSomeValuesFrom) {
			if (range instanceof String) {
				MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(schema, (String) range);
			} else if (range instanceof Schema) {
				MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(schema, (Schema) range);
			}
		} else if (or instanceof OWLObjectAllValuesFrom) {
			if (range instanceof String) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(schema, (String) range);
			} else if (range instanceof Schema) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(schema, (Schema) range);
			}
		} else if (or instanceof OWLObjectMinCardinality) {
			if (range instanceof String) {
				MapperObjectProperty.addMinCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (String) range);
			} else if (range instanceof Schema) {
				MapperObjectProperty.addMinCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (Schema) range);
			}
		} else if (or instanceof OWLObjectMaxCardinality) {
			if (range instanceof String) {
				MapperObjectProperty.addMaxCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (String) range);
			} else if (range instanceof Schema) {
				MapperObjectProperty.addMaxCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (Schema) range);
			}
		} else if (or instanceof OWLObjectExactCardinality) {
			if (range instanceof String) {
				MapperObjectProperty.addExactCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (String) range);
			} else if (range instanceof Schema) {
				MapperObjectProperty.addExactCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (Schema) range);
			}
		}
	}

	/**
	 * Applies data property restrictions to the schema based on the restriction type.
	 *
	 * @param schema the schema to update
	 * @param dr the data restriction
	 * @param range the range (either a String or Schema) to apply
	 * @param cardinality the cardinality value, if applicable
	 */
	static void applyDataRestriction(
			Schema schema,
			OWLQuantifiedDataRestriction dr,
			Object range,
			Integer cardinality,
			Logger logger) {
		if (dr instanceof OWLDataSomeValuesFrom) {
			if (range instanceof String) {
				MapperDataProperty.addSomeValuesFromToDataPropertySchema(schema, (String) range);
			} else if (range instanceof Schema) {
				MapperDataProperty.addSomeValuesFromToDataPropertySchema(schema, (Schema) range);
			}
		} else if (dr instanceof OWLDataAllValuesFrom) {
			if (range instanceof String) {
				MapperDataProperty.addAllOfDataPropertySchema(schema, (String) range);
			} else if (range instanceof Schema) {
				MapperDataProperty.addAllOfDataPropertySchema(schema, (Schema) range);
			}
		} else if (dr instanceof OWLDataMinCardinality) {
			if (range instanceof String) {
				MapperDataProperty.addMinCardinalityToPropertySchema(schema, cardinality, (String) range);
			} else {
				logger.log(
						Level.SEVERE,
						"applyDataRestriction(): Attempting to call addMinCardinalityToPropertySchema(), but"
								+ " the `range` object is not a String value.");
			}
		} else if (dr instanceof OWLDataMaxCardinality) {
			if (range instanceof String) {
				MapperDataProperty.addMaxCardinalityToPropertySchema(schema, cardinality, (String) range);
			} else {
				logger.log(
						Level.SEVERE,
						"applyDataRestriction(): Attempting to call addMaxCardinalityToPropertySchema(), but"
								+ " the `range` object is not a String value.");
			}
		} else if (dr instanceof OWLDataExactCardinality) {
			if (range instanceof String) {
				MapperDataProperty.addExactCardinalityToPropertySchema(schema, cardinality, (String) range);
			} else {
				logger.log(
						Level.SEVERE,
						"applyDataRestriction(): Attempting to call addExactCardinalityToPropertySchema(), but"
								+ " the `range` object is not a String value.");
			}
		}
	}
}
