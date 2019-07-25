package com.jbequinn.syncserver.infrastructure.service;

import com.jbequinn.syncserver.domain.model.ChangesDto;
import com.jbequinn.syncserver.infrastructure.repository.MongoRepository;
import lombok.extern.flogger.Flogger;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.flogger.LazyArgs.lazy;
import static com.jbequinn.syncserver.infrastructure.service.JsonAccessor.getLongValueOrZero;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;

@Flogger
public class JsonSyncServerService {
	private final MongoRepository repository;
	private final JsonObjectMerger merger;

	public JsonSyncServerService(MongoRepository repository, JsonObjectMerger merger) {
		this.repository = repository;
		this.merger = merger;
	}

	public JsonObject sync(JsonObject jsonObject) throws ExecutionException, InterruptedException, TimeoutException {
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

		CompletableFuture<Void> processTags = CompletableFuture
				.supplyAsync(() -> getExistingTags(tags))
				.thenAccept(existingTagsById -> addTagsToDto(tags, existingTagsById, changes));
		CompletableFuture<Void> processItems = CompletableFuture
				.supplyAsync(() -> getExistingItems(items))
				.thenAccept(existingItemsById -> addItemsToDto(items, existingItemsById, changes));

		allOf(processTags, processItems).get(2, TimeUnit.MINUTES);

		repository.sync(changes);

		// optimization: don't include in the response those elements sent
		var responseBuilder = createObjectBuilder()
				.add("sync_ts", updatedTimestamp)
				.add("success", true)
				.add("time_delta_ms", getLongValueOrZero(jsonObject, "time_delta_ms"));

		CompletableFuture<Void> findItemsNewer = CompletableFuture
				.supplyAsync(() -> repository.findItemsNewerThan(lastSync))
				.thenAccept(itemsNewer -> responseBuilder.add("items", itemsNewer));
		CompletableFuture<Void> findTagsNewer = CompletableFuture
				.supplyAsync(() -> repository.findTagsNewerThan(lastSync))
				.thenAccept(tagsNewer -> responseBuilder.add("tags", tagsNewer));
		CompletableFuture<Void> findDeletionsNewer = CompletableFuture
				.supplyAsync(() -> repository.findDeletionsNewerThan(lastSync))
				.thenAccept(deletionsNewer -> responseBuilder.add("deletions_to_add", deletionsNewer));

		allOf(findItemsNewer, findTagsNewer, findDeletionsNewer).get(2, TimeUnit.MINUTES);

		var response = responseBuilder.build();

		log.atFinest()
				.log("Response body: %s", lazy(() -> response));

		return response;
	}

	private Map<String, JsonObject> getExistingTags(List<JsonObject> tags) {
		var tagIds = tags.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		return repository.findTagsById(tagIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));
	}

	private void addTagsToDto(List<JsonObject> tags, Map<String, JsonObject> existingTagsById, ChangesDto changes) {
		tags.stream()
				.map(JsonValue::asJsonObject)
				.forEach(tag -> {
							var tagId = tag.getString("id");
							if (existingTagsById.containsKey(tagId)) {
								existingTagsById.put(tagId, merger.mergeTag(tag, existingTagsById.get(tagId)));
							} else {
								changes.getNewTagsToSave().add(tag);
							}
						}
				);

		changes.getTagsToUpdate().addAll(existingTagsById.values());
	}

	private Map<String, JsonObject> getExistingItems(List<JsonObject> items) {
		var itemIds = items.stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());

		return repository.findItemsById(itemIds).stream()
				.map(JsonValue::asJsonObject)
				.collect(toMap(object -> object.getString("id"), identity()));
	}

	private void addItemsToDto(List<JsonObject> items, Map<String, JsonObject> existingItemsById, ChangesDto changes) {
		items.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> {
							var itemId = item.getString("id");
							if (existingItemsById.containsKey(itemId)) {
								existingItemsById.put(itemId, merger.mergeItem(item, existingItemsById.get(itemId)));
							} else {
								changes.getNewItemsToSave().add(item);
							}
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
