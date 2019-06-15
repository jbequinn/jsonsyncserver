package com.jbequinn.jsonsyncserver.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.jsonsyncserver.domain.model.ChangesDto;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import lombok.extern.flogger.Flogger;
import org.bson.Document;
import org.springframework.lang.NonNull;
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

@Repository
@Flogger
public class MongoRepository {
	private final MongoCollection<Document> itemsCollection;
	private final MongoCollection<Document> tagsCollection;
	private final MongoCollection<Document> deletionsCollection;

	private final MongoClient mongoClient;
	private final ObjectMapper objectMapper;

	public MongoRepository(MongoClient mongoClient, ObjectMapper objectMapper) {
		this.objectMapper = requireNonNull(objectMapper);
		this.mongoClient = mongoClient;

		mongoClient.getDatabase("admin").runCommand("setFeatureCompatibilityVersion: 4.0");

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
			log.atFine().log("No ids to find in the collection %s", collection.getNamespace().getCollectionName());
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

	public void saveNewItems(List<JsonObject> items, ClientSession session) {
		saveInCollection(itemsCollection, items, session);
	}

	public void saveNewTags(List<JsonObject> tags) {
		saveInCollection(tagsCollection, tags);
	}

	public void saveNewTags(List<JsonObject> tags, ClientSession session) {
		saveInCollection(tagsCollection, tags, session);
	}

	public void saveNewDeletions(List<JsonObject> deletions) {
		saveInCollection(deletionsCollection, deletions);
	}

	public void saveNewDeletions(List<JsonObject> deletions, ClientSession session) {
		saveInCollection(deletionsCollection, deletions, session);
	}

	private void saveInCollection(MongoCollection<Document> collection, List<JsonObject> objects) {
		if (objects == null || objects.isEmpty()) {
			log.atFine().log("No elements to save in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}
		collection.insertMany(toDocuments(objects));
	}

	private void saveInCollection(MongoCollection<Document> collection, List<JsonObject> objects, ClientSession session) {
		if (objects == null || objects.isEmpty()) {
			log.atFine().log("No elements to save in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}
		collection.insertMany(session, toDocuments(objects));
	}

	public void updateExistingItems(List<JsonObject> items) {
		updateInCollection(itemsCollection, items);
	}

	public void updateExistingItems(List<JsonObject> items, ClientSession session) {
		updateInCollection(itemsCollection, items, session);
	}

	public void updateExistingTags(List<JsonObject> tags) {
		updateInCollection(tagsCollection, tags);
	}

	public void updateExistingTags(List<JsonObject> tags, ClientSession session) {
		updateInCollection(tagsCollection, tags, session);
	}

	private void updateInCollection(MongoCollection<Document> collection, List<JsonObject> jsonObjects) {
		if (jsonObjects == null || jsonObjects.isEmpty()) {
			log.atFine().log("No elements to update in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}

		jsonObjects.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> collection
						.replaceOne(eq("id", item.getString("id")), parse(item.toString())));
	}

	private void updateInCollection(MongoCollection<Document> collection, List<JsonObject> jsonObjects, ClientSession session) {
		if (jsonObjects == null || jsonObjects.isEmpty()) {
			log.atFine().log("No elements to update in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}

		jsonObjects.stream()
				.map(JsonValue::asJsonObject)
				.forEach(item -> collection
						.replaceOne(session, eq("id", item.getString("id")), parse(item.toString())));
	}

	public void sync(ChangesDto changes) {
		try (ClientSession session = mongoClient.startSession()) {
			saveNewItems(changes.getNewItemsToSave(), session);
			updateExistingItems(changes.getItemsToUpdate(), session);

			saveNewTags(changes.getNewTagsToSave(), session);
			updateExistingTags(changes.getTagsToUpdate(), session);

			saveNewDeletions(changes.getNewDeletions(), session);

			deleteInCollection(itemsCollection, changes.getItemsIdsToDelete(), session);
			deleteInCollection(tagsCollection, changes.getTagIdsToDelete(), session);

		}
	}

	private void deleteInCollection(MongoCollection<Document> collection, List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			log.atFine().log("No ids to delete in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}

		collection.deleteMany(in("id", ids));
	}

	private void deleteInCollection(MongoCollection<Document> collection, List<String> ids, ClientSession session) {
		if (ids == null || ids.isEmpty()) {
			log.atFine().log("No ids to delete in the collection %s", collection.getNamespace().getCollectionName());
			return;
		}

		collection.deleteMany(session, in("id", ids));
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

	@NonNull
	private JsonObject fromDocument(Document document) {
		document.remove("_id");
		try {
			return objectMapper.readValue(document.toJson(), JsonObject.class);
		} catch (IOException e) {
			log.atSevere()
					.withCause(e)
					.log("Error when reading the Mongo document: %s", document);
			return JsonValue.EMPTY_JSON_OBJECT;
		}
	}
}
