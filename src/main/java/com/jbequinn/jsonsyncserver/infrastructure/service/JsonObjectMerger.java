package com.jbequinn.jsonsyncserver.infrastructure.service;

import lombok.extern.flogger.Flogger;
import org.springframework.stereotype.Component;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.google.common.flogger.LazyArgs.lazy;
import static com.jbequinn.jsonsyncserver.infrastructure.service.JsonAccessor.getLongValueOrZero;

@Component
@Flogger
public class JsonObjectMerger {

	public JsonObject mergeItem(JsonObject one, JsonObject another) {
		log.atFinest()
				.log("Merging item: %s", lazy(() -> one));
		log.atFinest()
				.log("With the other item: %s", lazy(() -> another));

		var base = getMostRecentOf(one, another);

		var builder = Json.createObjectBuilder();
		base.forEach(builder::add);

		// there are probably more fields that can be merged
		builder.add("title", getMostRecentOfByKey(one, another, "title", "title_ts"));
		builder.add("completed_on", getMostRecentOfByKey(one, another, "completed_on", "completed_on_ts"));
		builder.add("due_date", getMostRecentOfByKey(one, another, "due_date", "due_date_ts"));
		builder.add("note", getMostRecentOfByKey(one, another, "note", "note_ts"));
		builder.add("start_date", getMostRecentOfByKey(one, another, "start_date", "start_date_ts"));
		builder.add("tags", getMostRecentOfByKey(one, another, "tags", "tags_changed_ts"));

		return builder.build();
	}

	public JsonObject mergeTag(JsonObject one, JsonObject another) {
		return getMostRecentOf(one, another);
	}

	private JsonValue getMostRecentOfByKey(JsonObject one, JsonObject another, String key, String timestampKey) {
		if (!one.containsKey(timestampKey)) {
			return another.get(key);
		}

		if (!another.containsKey(timestampKey)) {
			return one.get(key);
		}

		var timestampOne = getLongValueOrZero(one, timestampKey);
		var timestampAnother = getLongValueOrZero(another, timestampKey);

		return timestampOne > timestampAnother ? one.get(key) : another.get(key);
	}

	private JsonObject getMostRecentOf(JsonObject one, JsonObject another) {
		var timestampOne = getLongValueOrZero(one, "changed_ts");
		var timestampAnother = getLongValueOrZero(another, "changed_ts");

		return timestampOne > timestampAnother ? one : another;
	}
}
