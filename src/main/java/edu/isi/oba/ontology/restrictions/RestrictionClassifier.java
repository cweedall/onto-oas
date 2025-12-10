package edu.isi.oba.ontology.restrictions;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

public class RestrictionClassifier {
	private RestrictionClassifier() {
		throw new UnsupportedOperationException("Static utility class");
	}

	public static RestrictionKind classify(OWLClassExpression expr) {
		if (expr instanceof OWLObjectSomeValuesFrom) return RestrictionKind.OBJECT_SOME_VALUES_FROM;
		if (expr instanceof OWLObjectAllValuesFrom) return RestrictionKind.OBJECT_ALL_VALUES_FROM;
		if (expr instanceof OWLObjectMinCardinality) return RestrictionKind.OBJECT_MIN_CARDINALITY;
		if (expr instanceof OWLObjectMaxCardinality) return RestrictionKind.OBJECT_MAX_CARDINALITY;
		if (expr instanceof OWLObjectExactCardinality) return RestrictionKind.OBJECT_EXACT_CARDINALITY;
		if (expr instanceof OWLObjectHasValue) return RestrictionKind.OBJECT_HAS_VALUE;
		if (expr instanceof OWLObjectOneOf) return RestrictionKind.OBJECT_ONE_OF;
		if (expr instanceof OWLObjectComplementOf) return RestrictionKind.OBJECT_COMPLEMENT_OF;
		if (expr instanceof OWLObjectIntersectionOf) return RestrictionKind.OBJECT_INTERSECTION_OF;
		if (expr instanceof OWLObjectUnionOf) return RestrictionKind.OBJECT_UNION_OF;

		if (expr instanceof OWLDataSomeValuesFrom) return RestrictionKind.DATA_SOME_VALUES_FROM;
		if (expr instanceof OWLDataAllValuesFrom) return RestrictionKind.DATA_ALL_VALUES_FROM;
		if (expr instanceof OWLDataMinCardinality) return RestrictionKind.DATA_MIN_CARDINALITY;
		if (expr instanceof OWLDataMaxCardinality) return RestrictionKind.DATA_MAX_CARDINALITY;
		if (expr instanceof OWLDataExactCardinality) return RestrictionKind.DATA_EXACT_CARDINALITY;
		if (expr instanceof OWLDataHasValue) return RestrictionKind.DATA_HAS_VALUE;
		if (expr instanceof OWLDataOneOf) return RestrictionKind.DATA_ONE_OF;
		if (expr instanceof OWLDataComplementOf) return RestrictionKind.DATA_COMPLEMENT_OF;
		if (expr instanceof OWLDataIntersectionOf) return RestrictionKind.DATA_INTERSECTION_OF;
		if (expr instanceof OWLDataUnionOf) return RestrictionKind.DATA_UNION_OF;

		return RestrictionKind.UNKNOWN;
	}
}
