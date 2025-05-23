package edu.isi.oba.config.paths;

import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.generators.CardinalityType;
import edu.isi.oba.generators.HttpMethod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum OperationType {
	DELETE_BY_KEY(HttpMethod.DELETE, ConfigPropertyNames.DELETE_BY_KEY, CardinalityType.SINGULAR),
	GET_ALL(HttpMethod.GET, ConfigPropertyNames.GET_ALL, CardinalityType.PLURAL),
	GET_BY_KEY(HttpMethod.GET, ConfigPropertyNames.GET_BY_KEY, CardinalityType.SINGULAR),
	POST_BULK(HttpMethod.POST, ConfigPropertyNames.POST_BULK, CardinalityType.PLURAL),
	POST_SINGLE(HttpMethod.POST, ConfigPropertyNames.POST_SINGLE, CardinalityType.SINGULAR),
	PUT_BULK(HttpMethod.PUT, ConfigPropertyNames.PUT_BULK, CardinalityType.PLURAL),
	PUT_BY_KEY(HttpMethod.PUT, ConfigPropertyNames.PUT_BY_KEY, CardinalityType.SINGULAR),
	SEARCH_BY_POST(HttpMethod.SEARCH, ConfigPropertyNames.SEARCH_BY_POST, CardinalityType.PLURAL);

	private final HttpMethod method;
	private final CardinalityType cardinality;
	private final String name;
	private static final Map<HttpMethod, Set<OperationType>> HTTP_METHOD_GROUP = new HashMap<>();
	private static final Map<CardinalityType, Set<OperationType>> CARDINALITY_GROUP = new HashMap<>();
	private static final Map<String, OperationType> BY_LABEL = new HashMap<>();

	static {
		for (OperationType e : values()) {
			HTTP_METHOD_GROUP.computeIfAbsent(e.method, k -> new HashSet<>()).add(e);
			CARDINALITY_GROUP.computeIfAbsent(e.cardinality, k -> new HashSet<>()).add(e);
			BY_LABEL.put(e.name, e);
		}
	}

	private OperationType(HttpMethod method, String name, CardinalityType cardinality) {
		this.method = method;
		this.cardinality = cardinality;
		this.name = name;
	}

	public HttpMethod getHttpMethod() {
		return this.method;
	}

	public CardinalityType getCardinalityType() {
		return this.cardinality;
	}

	public static Set<OperationType> getOperationTypes(HttpMethod method) {
		return HTTP_METHOD_GROUP.get(method);
	}

	public static Set<OperationType> getOperationTypes(CardinalityType cardinality) {
		return CARDINALITY_GROUP.get(cardinality);
	}

	public static OperationType valueOfLabel(String label) {
		return BY_LABEL.get(label);
	}
}
