package com.jbequinn.syncserver.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Arrays;
import static com.jbequinn.syncserver.infrastructure.service.JsonAccessor.getLongValueOrZero;

public class JsonObjectMerger {

	private Logger logger = LoggerFactory.getLogger(JsonObjectMerger.class);

	private static final String[][] PROPERTIES = new String[][]{
			// there are probably more fields that can be merged
			{"title", "title_ts"},
			{"completed_on", "completed_on_ts"},
			{"due_date", "due_date_ts"},
			{"note", "note_ts"},
			{"start_date", "start_date_ts"},
			{"tags", "tags_changed_ts"},
	};

	public JsonObject mergeItem(JsonObject one, JsonObject another) {
		logger.debug("Merging item: {}", one);
		logger.debug("With the other item: {}", another);

		var base = getMostRecentOf(one, another);

		var builder = Json.createObjectBuilder();
		base.forEach(builder::add);

		Arrays.stream(PROPERTIES)
				.filter(property -> one.get(property[0]) != null || another.get(property[0]) != null)
				.forEach(property -> updatePropertyInBuilder(builder, one, another, property[0], property[1]));

		return builder.build();
	}

	public JsonObject mergeTag(JsonObject one, JsonObject another) {
		return getMostRecentOf(one, another);
	}

	private void updatePropertyInBuilder(JsonObjectBuilder builder, JsonObject one, JsonObject another, String key, String timestampKey) {
		if (!one.containsKey(timestampKey)) {
			builder.add(key, another.get(key));
			builder.add(timestampKey, another.get(timestampKey));
			return;
		}

		if (!another.containsKey(timestampKey)) {
			builder.add(key, one.get(key));
			builder.add(timestampKey, one.get(timestampKey));
			return;
		}

		var timestampOne = getLongValueOrZero(one, timestampKey);
		var timestampAnother = getLongValueOrZero(another, timestampKey);

		builder.add(key, timestampOne > timestampAnother ? one.get(key) : another.get(key));
		builder.add(timestampKey, Math.max(timestampOne, timestampAnother));
	}

	private JsonObject getMostRecentOf(JsonObject one, JsonObject another) {
		var timestampOne = getLongValueOrZero(one, "changed_ts");
		var timestampAnother = getLongValueOrZero(another, "changed_ts");

		return timestampOne > timestampAnother ? one : another;
	}
}
