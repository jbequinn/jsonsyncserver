package com.jbequinn.jsonsyncserver.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.bson.Document.parse;

@Slf4j
@Repository
public class MongoRepository {
	private final MongoCollection<Document> itemsCollection;
	private final MongoCollection<Document> tagsCollection;
	private final MongoCollection<Document> deletionsCollection;

	private final ObjectMapper objectMapper;

	public MongoRepository(MongoClient mongoClient, ObjectMapper objectMapper) {
		this.objectMapper = requireNonNull(objectMapper);

		var database = mongoClient.getDatabase("jsondb");

		itemsCollection = database.getCollection("items");
		tagsCollection = database.getCollection("tags");
		deletionsCollection = database.getCollection("deletion");

		itemsCollection.createIndex(Indexes.ascending("id"));
		tagsCollection.createIndex(Indexes.ascending("id"));
	}

	public JsonArray findAllItems() {
		return findAllByCollection(itemsCollection);
	}

	public JsonArray findAllTags() {
		return findAllByCollection(tagsCollection);
	}

	public JsonArray findAllDeletions() {
		return findAllByCollection(deletionsCollection);
	}

	public List<JsonObject> findItemsById(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return findInCollectionById(itemsCollection, ids);
	}

	public List<JsonObject> findTagsById(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}

		return findInCollectionById(tagsCollection, ids);
	}

	private List<JsonObject> findInCollectionById(MongoCollection<Document> collection, List<String> ids) {
		return collection.find()
				.filter(in("id", ids))
				.map(this::fromDocument)
				.into(new ArrayList<>());
	}

	public JsonArray findItemsNewerThan(long timestamp) {
		var builder = Json.createArrayBuilder();
		itemsCollection.find()
				.filter(or(
						gt("changed_ts", timestamp),
						gt("title_ts", timestamp),
						gt("created_on", timestamp)
				))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public JsonArray findTagsNewerThan(long timestamp) {
		var builder = Json.createArrayBuilder();
		tagsCollection.find()
				.filter(or(
						gt("changed_ts", timestamp),
						gt("created_on", timestamp)
				))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public JsonArray findDeletionsNewerThan(long timestamp) {
		var builder = Json.createArrayBuilder();
		deletionsCollection.find()
				//.filter(and(gt("ts", timestamp), not(in("sync_id", ids))))
				.filter(and(gt("ts", timestamp)))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public void saveNewItems(List<JsonObject> items) {
		if (items != null && !items.isEmpty()) {
			itemsCollection.insertMany(toDocuments(items));
		}
	}

	public void saveNewTags(List<JsonObject> tags) {
		if (tags != null && !tags.isEmpty()) {
			tagsCollection.insertMany(toDocuments(tags));
		}
	}

	public void saveNewDeletions(List<JsonObject> deletions) {
		if (deletions != null && !deletions.isEmpty()) {
			deletionsCollection.insertMany(toDocuments(deletions));
		}
	}

	public void updateExistingItems(List<JsonObject> items) {
		updateInCollection(items, itemsCollection);
	}

	public void updateExistingTags(List<JsonObject> tags) {
		updateInCollection(tags, tagsCollection);
	}

	public void sync(ChangesDto changes) {
		saveNewItems(changes.getNewItemsToSave());
		updateExistingItems(changes.getItemsToUpdate());

		saveNewTags(changes.getNewTagsToSave());
		updateExistingTags(changes.getTagsToUpdate());

		saveNewDeletions(changes.getNewDeletions());

		if (changes.getItemsIdsToDelete() != null && !changes.getItemsIdsToDelete().isEmpty()) {
			itemsCollection.deleteMany(in("id", changes.getItemsIdsToDelete()));
		}

		if (changes.getTagIdsToDelete() != null && !changes.getTagIdsToDelete().isEmpty()) {
			tagsCollection.deleteMany(in("id", changes.getTagIdsToDelete()));
		}
	}

	private void updateInCollection(List<JsonObject> jsonObjects, MongoCollection<Document> collection) {
		if (jsonObjects != null && !jsonObjects.isEmpty()) {
			jsonObjects.stream()
					.map(JsonValue::asJsonObject)
					.forEach(item -> itemsCollection
							.replaceOne(eq("id", item.getString("id")), parse(item.toString())));
		}
	}

	public void deleteAllItems() {
		itemsCollection.deleteMany(new Document());
	}

	public void deleteAllTags() {
		tagsCollection.deleteMany(new Document());
	}

	public void deleteAllDeletions() {
		deletionsCollection.deleteMany(new Document());
	}

	private JsonArray findAllByCollection(MongoCollection<Document> collection) {
		var builder = Json.createArrayBuilder();
		collection.find()
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	private List<Document> toDocuments(List<JsonObject> jsonArray) {
		return jsonArray.stream()
				.map(jsonObject -> parse(jsonObject.toString()))
				.collect(toList());
	}

	private JsonObject fromDocument(Document document) {
		document.remove("_id");
		try {
			return objectMapper.readValue(document.toJson(), JsonObject.class);
		} catch (IOException e) {
			return JsonValue.EMPTY_JSON_OBJECT;
		}
	}
}
