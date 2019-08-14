package com.jbequinn.syncserver.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.syncserver.domain.model.ChangesDto;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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

public class MongoRepository {
	private Logger logger = LoggerFactory.getLogger(MongoRepository.class);

	private final MongoCollection<Document> itemsCollection;
	private final MongoCollection<Document> tagsCollection;
	private final MongoCollection<Document> deletionsCollection;

	private final ObjectMapper objectMapper;

	public MongoRepository(MongoClient mongoClient, ObjectMapper objectMapper) {
		this.objectMapper = requireNonNull(objectMapper);

		var database = mongoClient.getDatabase("everdo");

		itemsCollection = database.getCollection("items");
		tagsCollection = database.getCollection("tags");
		deletionsCollection = database.getCollection("deletions");

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
		return findInCollectionById(itemsCollection, ids);
	}

	public List<JsonObject> findTagsById(List<String> ids) {
		return findInCollectionById(tagsCollection, ids);
	}

	private List<JsonObject> findInCollectionById(MongoCollection<Document> collection, List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			logger.info("No ids to find in the collection {}", collection.getNamespace().getCollectionName());
			return List.of();
		}

		return collection.find()
				.filter(in("id", ids))
				.map(this::fromDocument)
				.into(new ArrayList<>());
	}

	public JsonArray findItemsNewerThan(long timestamp) {
		return findInCollectionNewerThan(itemsCollection, timestamp);
	}

	public JsonArray findTagsNewerThan(long timestamp) {
		return findInCollectionNewerThan(tagsCollection, timestamp);
	}

	private JsonArray findInCollectionNewerThan(MongoCollection<Document> collection, long timestamp) {
		var builder = Json.createArrayBuilder();
		collection.find()
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
				.filter(gt("ts", timestamp))
				.map(this::fromDocument)
				.forEach((Consumer<JsonObject>) builder::add);

		return builder.build();
	}

	public void saveNewItems(List<JsonObject> items) {
		saveInCollection(itemsCollection, items);
	}

	public void saveNewTags(List<JsonObject> tags) {
		saveInCollection(tagsCollection, tags);
	}

	public void saveNewDeletions(List<JsonObject> deletions) {
		saveInCollection(deletionsCollection, deletions);
	}

	private void saveInCollection(MongoCollection<Document> collection, List<JsonObject> objects) {
		if (objects == null || objects.isEmpty()) {
			logger.info("No elements to save in the collection {}", collection.getNamespace().getCollectionName());
			return;
		}
		collection.insertMany(toDocuments(objects));
	}

	public void updateExistingItems(List<JsonObject> items) {
		updateInCollection(itemsCollection, items);
	}

	public void updateExistingTags(List<JsonObject> tags) {
		updateInCollection(tagsCollection, tags);
	}

	private void updateInCollection(MongoCollection<Document> collection, List<JsonObject> jsonObjects) {
		if (jsonObjects == null || jsonObjects.isEmpty()) {
			logger.info("No elements to update in the collection {}", collection.getNamespace().getCollectionName());
			return;
		}

		jsonObjects.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> collection
						.replaceOne(eq("id", item.getString("id")), parse(item.toString())));
	}

	public void sync(ChangesDto changes) {
		saveNewItems(changes.getNewItemsToSave());
		updateExistingItems(changes.getItemsToUpdate());

		saveNewTags(changes.getNewTagsToSave());
		updateExistingTags(changes.getTagsToUpdate());

		saveNewDeletions(changes.getNewDeletions());

		deleteInCollection(itemsCollection, changes.getItemsIdsToDelete());
		deleteInCollection(tagsCollection, changes.getTagIdsToDelete());
	}

	private void deleteInCollection(MongoCollection<Document> collection, List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			logger.info("No ids to delete in the collection {}", collection.getNamespace().getCollectionName());
			return;
		}

		collection.deleteMany(in("id", ids));
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
			logger.error("Error when reading the Mongo document: {}", document);
			return JsonValue.EMPTY_JSON_OBJECT;
		}
	}
}
