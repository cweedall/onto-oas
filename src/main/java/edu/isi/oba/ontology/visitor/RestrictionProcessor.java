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

	private final VisitorContext context;
	private final Logger logger;
	private final OWLObjectVisitor visitor;

	public RestrictionProcessor(VisitorContext context, Logger logger, OWLObjectVisitor visitor) {
		this.context = context;
		this.logger = logger;
		this.visitor = visitor;
	}

	/**
	 * Processes quantified object restrictions such as someValuesFrom, allValuesFrom, and
	 * cardinalities.
	 *
	 * @param or the OWL quantified object restriction
	 */
	public void processQuantifiedObjectRestriction(OWLQuantifiedObjectRestriction or) {
		Schema schema = getOrCreateSchema();
		OWLClassExpression filler = or.getFiller();

		if (filler instanceof OWLObjectOneOf) {
			filler.accept(visitor);
		} else if (filler instanceof OWLNaryBooleanClassExpression) {
			Schema composed =
					MapperObjectProperty.getComplexObjectComposedSchema(
							(OWLNaryBooleanClassExpression) filler);
			applyObjectRestriction(schema, or, composed);
		} else {
			String range =
					SchemaBuilder.getPrefixedSchemaName(filler.asOWLClass(), context.getBaseClassOntology());
			applyObjectRestriction(schema, or, range);
		}
	}

	/**
	 * Processes quantified data restrictions such as someValuesFrom, allValuesFrom, and
	 * cardinalities.
	 *
	 * @param dr the OWL quantified data restriction
	 */
	public void processQuantifiedDataRestriction(OWLQuantifiedDataRestriction dr) {
		Schema schema = getOrCreateSchema();
		OWLDataRange filler = dr.getFiller();
		Integer cardinality =
				(dr instanceof OWLDataCardinalityRestriction)
						? ((OWLDataCardinalityRestriction) dr).getCardinality()
						: null;

		if (filler instanceof OWLDataOneOf) {
			filler.accept(visitor);
		} else if (filler instanceof OWLNaryDataRange) {
			Schema composed = MapperDataProperty.getComplexDataComposedSchema((OWLNaryDataRange) filler);
			applyDataRestriction(schema, dr, composed, cardinality);
		} else if (filler instanceof OWLDatatypeRestriction) {
			OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) filler;
			String range = restriction.getDatatype().getIRI().getShortForm();
			for (OWLFacetRestriction facet : restriction.getFacetRestrictions()) {
				applyDataRestriction(schema, dr, range, cardinality);
				MapperDataProperty.addDatatypeRestrictionToPropertySchema(schema, facet);
			}
		} else {
			String range = filler.asOWLDatatype().getIRI().getShortForm();
			applyDataRestriction(schema, dr, range, cardinality);
		}
	}

	/**
	 * Processes n-ary boolean class expressions (e.g., unionOf, intersectionOf) for object
	 * properties.
	 *
	 * @param ce the OWL n-ary boolean class expression
	 */
	public void processNaryBooleanClassExpression(OWLNaryBooleanClassExpression ce) {
		Schema schema = getOrCreateSchema();
		schema.setItems(MapperObjectProperty.getComplexObjectComposedSchema(ce));
		MapperProperty.setSchemaType(schema, "array");
	}

	/**
	 * Processes n-ary data ranges (e.g., unionOf, intersectionOf) for data properties.
	 *
	 * @param ce the OWL n-ary data range
	 */
	public void processNaryDataRange(OWLNaryDataRange ce) {
		Schema schema = getOrCreateSchema();
		schema.setItems(MapperDataProperty.getComplexDataComposedSchema(ce));
		MapperProperty.setSchemaType(schema, "array");
	}

	/**
	 * Processes hasValue restrictions for object or data properties.
	 *
	 * @param ce the OWL hasValue restriction
	 */
	public void processHasValue(OWLObjectHasValue ce) {
		Schema schema = getOrCreateSchema();
		if (ce.getFiller() instanceof OWLNamedIndividual) {
			MapperObjectProperty.addHasValueOfPropertySchema(schema, ce.getFiller());
		}
	}

	/**
	 * Processes hasValue restrictions for object or data properties.
	 *
	 * @param ce the OWL hasValue restriction
	 */
	public void processHasValue(OWLDataHasValue ce) {
		Schema schema = getOrCreateSchema();
		if (ce.getFiller().getDatatype() != null) {
			MapperDataProperty.addHasValueOfPropertySchema(schema, ce.getFiller());
		}
	}

	/**
	 * Processes oneOf enumerations for object or data properties.
	 *
	 * @param ce the OWL oneOf restriction
	 */
	public void processOneOf(OWLObjectOneOf ce) {
		Schema schema = getOrCreateSchema();
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
	public void processOneOf(OWLDataOneOf ce) {
		Schema schema = getOrCreateSchema();
		ce.values().forEach(value -> MapperDataProperty.addOneOfDataPropertySchema(schema, value));
	}

	/**
	 * Processes complementOf restrictions for object or data properties.
	 *
	 * @param ce the OWL complementOf restriction
	 */
	public void processComplementOf(OWLObjectComplementOf ce) {
		Schema schema = getOrCreateSchema();
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
	public void processComplementOf(OWLDataComplementOf ce) {
		Schema schema = getOrCreateSchema();
		ce.datatypesInSignature()
				.forEach(dt -> MapperDataProperty.setComplementOfForDataSchema(schema, dt));
	}

	Schema getOrCreateSchema() {
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
	void applyObjectRestriction(Schema schema, OWLQuantifiedObjectRestriction or, Object range) {
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
			} else {
				logger.log(
						Level.SEVERE,
						"applyObjectRestriction(): Attempting to call addMinCardinalityToPropertySchema(), but"
								+ " the `range` object is not a String value.");
			}
		} else if (or instanceof OWLObjectMaxCardinality) {
			if (range instanceof String) {
				MapperObjectProperty.addMaxCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (String) range);
			} else {
				logger.log(
						Level.SEVERE,
						"applyObjectRestriction(): Attempting to call addMaxCardinalityToPropertySchema(), but"
								+ " the `range` object is not a String value.");
			}
		} else if (or instanceof OWLObjectExactCardinality) {
			if (range instanceof String) {
				MapperObjectProperty.addExactCardinalityToPropertySchema(
						schema, ((OWLObjectCardinalityRestriction) or).getCardinality(), (String) range);
			} else {
				logger.log(
						Level.SEVERE,
						"applyObjectRestriction(): Attempting to call addExactCardinalityToPropertySchema(),"
								+ " but the `range` object is not a String value.");
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
	void applyDataRestriction(
			Schema schema, OWLQuantifiedDataRestriction dr, Object range, Integer cardinality) {
		if (dr instanceof OWLDataSomeValuesFrom) {
			if (range instanceof String) {
				MapperDataProperty.addSomeValuesFromToDataPropertySchema(schema, (String) range);
			} else if (range instanceof Schema) {
				MapperDataProperty.addSomeValuesFromToDataPropertySchema(schema, (Schema) range);
			}
		} else if (dr instanceof OWLDataAllValuesFrom) {
			MapperDataProperty.addAllOfDataPropertySchema(schema, (String) range);
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
