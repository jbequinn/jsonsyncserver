package com.jbequinn.jsonsyncserver.infrastructure.service;

import com.jbequinn.jsonsyncserver.domain.model.ChangesDto;
import com.jbequinn.jsonsyncserver.infrastructure.repository.MongoRepository;
import lombok.extern.flogger.Flogger;
import org.springframework.stereotype.Service;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.google.common.flogger.LazyArgs.lazy;
import static com.jbequinn.jsonsyncserver.infrastructure.service.JsonAccessor.getLongValueOrZero;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;

@Service
@Flogger
public class JsonSyncServerService {
	private final MongoRepository repository;
	private final JsonObjectMerger merger;

	public JsonSyncServerService(MongoRepository repository, JsonObjectMerger merger) {
		this.repository = repository;
		this.merger = merger;
	}

	public JsonObject sync(JsonObject jsonObject) {
		log.atFinest()
				.log("Request body: %s", lazy(() -> jsonObject));

		var lastSync = getLongValueOrZero(jsonObject, "last_sync_ts");

		var updatedTimestamp = Instant.now().getEpochSecond();

		var tags = jsonObject.get("changes").asJsonObject().getJsonArray("tags").getValuesAs(JsonObject.class);
		var items = jsonObject.get("changes").asJsonObject().getJsonArray("items").getValuesAs(JsonObject.class);
		var deletions = jsonObject.get("changes").asJsonObject().getJsonArray("deletions");

		var changes = new ChangesDto();

		changes.setNewDeletions(jsonObject.get("changes")
				.asJsonObject()
				.getJsonArray("deletions")
				.stream()
				.map(JsonValue::asJsonObject)
				.collect(toList()));

		changes.setItemsIdsToDelete(deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(element -> "i".equals(element.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList()));

		changes.setTagIdsToDelete(deletions.stream()
				.map(JsonValue::asJsonObject)
				.filter(jsonObject1 -> "t".equals(jsonObject1.getString("entity_type")))
				.map(element -> element.getString("sync_id"))
				.collect(toList()));

		addItemsToDto(items, changes);
		addTagsToDto(tags, changes);

		repository.sync(changes);

		// optimization: don't include in the response those elements sent
		var response = createObjectBuilder()
				.add("sync_ts", updatedTimestamp)
				.add("items", repository.findItemsNewerThan(lastSync))
				.add("tags", repository.findTagsNewerThan(lastSync))
				.add("deletions_to_add", repository.findDeletionsNewerThan(lastSync))
				.add("success", true)
				.add("time_delta_ms", getLongValueOrZero(jsonObject, "time_delta_ms"))
				.build();

		log.atFinest()
				.log("Response body: %s", lazy(() -> response));

		return response;
	}

	private void addTagsToDto(List<JsonObject> tags, ChangesDto changes) {
		var tagIds = tags.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		var existingTagsById = repository.findTagsById(tagIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));

		tags.stream()
				.map(JsonValue::asJsonObject)
				.forEach(tag -> {
							existingTagsById.computeIfPresent(tag.getString("id"),
									(key, jsonObject) -> merger.mergeTag(tag, jsonObject));
							existingTagsById.computeIfAbsent(tag.getString("id"), key -> {
								changes.getNewTagsToSave().add(tag);
								return null;
							});
						}
				);

		changes.getTagsToUpdate().addAll(existingTagsById.values());
	}

	private void addItemsToDto(List<JsonObject> items, ChangesDto changes) {
		var itemIds = items.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		var existingItemsById = repository.findItemsById(itemIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));

		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> {
							existingItemsById.computeIfPresent(item.getString("id"),
									(key, jsonObject) -> merger.mergeItem(item, jsonObject));
							existingItemsById.computeIfAbsent(item.getString("id"), key -> {
								changes.getNewItemsToSave().add(item);
								return null;
							});
						}
				);

		changes.getItemsToUpdate().addAll(existingItemsById.values());
	}

	public JsonObject findAllItemsTagsAndDeletions() {
		return createObjectBuilder()
				.add("items", repository.findAllItems())
				.add("tags", repository.findAllTags())
				.add("deletions", repository.findAllDeletions())
				.build();
	}

	public void saveAllItemsTagsAndDeletions(JsonObject jsonObject) {
		var items = Objects.requireNonNull(jsonObject.getJsonArray("items").getValuesAs(JsonObject.class));
		var tags = Objects.requireNonNull(jsonObject.getJsonArray("tags").getValuesAs(JsonObject.class));
		var deletions = Objects.requireNonNull(jsonObject.getJsonArray("deletions").getValuesAs(JsonObject.class));

		deleteAllItemsTagsAndDeletions();
		repository.saveNewItems(items);
		repository.saveNewTags(tags);
		repository.saveNewDeletions(deletions);
	}

	public void deleteAllItemsTagsAndDeletions() {
		repository.deleteAllItems();
		repository.deleteAllTags();
		repository.deleteAllDeletions();
	}
}
