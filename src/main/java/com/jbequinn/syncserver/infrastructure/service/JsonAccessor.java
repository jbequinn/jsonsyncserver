package com.jbequinn.syncserver.infrastructure.service;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class JsonAccessor {
	private JsonAccessor(){}

	public static long getLongValueOrZero(JsonObject jsonObject, String key) {
		var value = 0L;
		var jsonValue = jsonObject.get(key);
		if (jsonValue != null && jsonValue.getValueType() == JsonValue.ValueType.NUMBER) {
			value = ((JsonNumber)jsonValue).longValue();
		}
		return value;
	}
}
