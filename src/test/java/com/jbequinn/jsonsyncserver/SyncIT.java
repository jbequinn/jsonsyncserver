package com.jbequinn.jsonsyncserver;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.StringReader;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncIT extends ITBase {
	@BeforeEach
	void setUp() throws Exception {
		when()
				.post("/wipe")
			.then().assertThat()
				.statusCode(HTTP_OK);

		given()
				.body(ClassLoader.getSystemResourceAsStream("file-simple.json").readAllBytes())
				.when()
				.post("/push")
				.then().assertThat()
				.statusCode(HTTP_OK);
	}

	@Test
	void timeEndpoint() {
		var before = Instant.now().toEpochMilli();

		long time = when()
					.get("/time")
				.then().assertThat()
					.statusCode(HTTP_OK)
					.extract().jsonPath().getLong("server_time_ms");

		assertThat(time).isBetween(before, Instant.now().toEpochMilli());
	}

	@Test
	void pullEndpoint() {
		String responseString = when()
					.post("/pull")
				.then().assertThat()
					.statusCode(HTTP_OK)
					.extract().body().asString();

		JsonObject response;
		try (var jsonReader = Json.createReader(new StringReader(responseString))) {
			response = jsonReader.readObject();
		}

		SoftAssertions.assertSoftly(softly -> {
			// AND all the previously existing items are returned in the response
			var itemIds = response.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds)
					.containsExactlyInAnyOrder(
							"801244036F944E7D808F5F157EED93B0", "CE18D30F61E44C2FA6C1F6FA8024E407",
							"12B7DA5E9EC146B493056384EA89E55D"
					);

			// AND all the previously existing tags are returned in the response
			var tagIds = response.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds)
					.containsExactlyInAnyOrder(
							"9B6204D188F6489799876DAB20539864", "5D1204D188F6489799876DAB2053978E"
					);

			// AND all the previously existing deletions are returned in the response
			var deletionIds = response.getJsonArray("deletions").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds)
					.containsExactlyInAnyOrder(
							"D2D53F1A4D184C9BA459D6444152E0231533592800", "D2D53F1A4D184C9BA459D6444152E0231525644000",
							"BD651DDD391145F9B78C2980B2D547F7", "E6BBAC422A4C48F793E75FBBF9CAEE35"
					);
		});
	}

	@Test
	void syncNoChangesNullTimestamp()  {
		var before = Instant.now().getEpochSecond();

		// GIVEN a sync request object with no new data, and no previous synchronization timestamp
		var responseString = given()
					.body(Json.createObjectBuilder()
							.add("last_sync_ts", "null")
							.add("changes", Json.createObjectBuilder()
									.add("items", JsonValue.EMPTY_JSON_ARRAY)
									.add("tags", JsonValue.EMPTY_JSON_ARRAY)
									.add("deletions", JsonValue.EMPTY_JSON_ARRAY)
									.build())
							.build().toString())
				// WHEN performing the request
				.when()
					.post("/sync")
				.then().assertThat()
					.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response;
		try (var jsonReader = Json.createReader(new StringReader(responseString))) {
			response = jsonReader.readObject();
		}

		var after = Instant.now().getEpochSecond();

		SoftAssertions.assertSoftly(softly -> {
			// THEN the request was successful
			softly.assertThat(response.get("success").toString()).isEqualTo("true");

			// AND the response now includes a timestamp
			var syncTs = response.getJsonNumber("sync_ts").longValue();
			softly.assertThat(syncTs).isBetween(before, after);

			// AND all the previously existing items are returned in the response
			var itemIds = response.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds)
					.containsExactlyInAnyOrder(
							"801244036F944E7D808F5F157EED93B0", "CE18D30F61E44C2FA6C1F6FA8024E407",
							"12B7DA5E9EC146B493056384EA89E55D"
					);

			// AND all the previously existing tags are returned in the response
			var tagIds = response.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds)
					.containsExactlyInAnyOrder(
							"9B6204D188F6489799876DAB20539864", "5D1204D188F6489799876DAB2053978E"
					);

			// AND all the previously existing deletions are returned in the response
			var deletionIds = response.getJsonArray("deletions_to_add").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds)
					.containsExactlyInAnyOrder(
							"D2D53F1A4D184C9BA459D6444152E0231533592800", "D2D53F1A4D184C9BA459D6444152E0231525644000",
							"BD651DDD391145F9B78C2980B2D547F7", "E6BBAC422A4C48F793E75FBBF9CAEE35"
					);
		});
	}

	@Test
	void syncFutureTimestampReturnsEmpty() {
		String responseString = given()
				.body(Json.createObjectBuilder()
						.add("last_sync_ts", 2000000000L)
						.add("changes", Json.createObjectBuilder()
								.add("items", JsonValue.EMPTY_JSON_ARRAY)
								.add("tags", JsonValue.EMPTY_JSON_ARRAY)
								.add("deletions", JsonValue.EMPTY_JSON_ARRAY)
								.build())
						.build().toString())
				.when()
				.post("/sync")
				.then().assertThat()
				.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response;
		try (var jsonReader = Json.createReader(new StringReader(responseString))) {
			response = jsonReader.readObject();
		}

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(response.get("success").toString()).isEqualTo("true");

			var itemIds = response.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds).isEmpty();

			var tagIds = response.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds).isEmpty();

			var deletionIds = response.getJsonArray("deletions_to_add").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds).isEmpty();
		});
	}

	@Test
	void syncOnelementAddedReturnsNewer() throws Exception {
		// GIVEN a sync request object
		var responseString = given()
				.body(Json.createObjectBuilder()
						.add("last_sync_ts", 1525128954)
						.add("changes", Json.createObjectBuilder()
								.add("items", Json.createArrayBuilder()
										.add(Json.createObjectBuilder()
												.add("id", "AA56D30F61E44C2FA6C1F6FA8024A001")
												.add("changed_ts", 1525138953)
												.add("created_on", 1525117953)
												.build())
										.build())
								.add("tags", JsonValue.EMPTY_JSON_ARRAY)
								.add("deletions", JsonValue.EMPTY_JSON_ARRAY)
								.build())
						.build().toString())
				// WHEN performing the request
				.when()
				.post("/sync")
				.then().assertThat()
				.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response;
		try (var jsonReader = Json.createReader(new StringReader(responseString))) {
			response = jsonReader.readObject();
		}

		SoftAssertions.assertSoftly(softly -> {
			// THEN the request was successful
			softly.assertThat(response.get("success").toString()).isEqualTo("true");

			// AND all the previously existing items are returned in the response
			var itemIds = response.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds)
					.containsExactlyInAnyOrder(
							"CE18D30F61E44C2FA6C1F6FA8024E407", "12B7DA5E9EC146B493056384EA89E55D", "AA56D30F61E44C2FA6C1F6FA8024A001"
					);

			// AND all the previously existing tags are returned in the response
			var tagIds = response.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds)
					.containsExactlyInAnyOrder(
							"5D1204D188F6489799876DAB2053978E"
					);

			// AND all the previously existing deletions are returned in the response
			var deletionIds = response.getJsonArray("deletions_to_add").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds)
					.containsExactlyInAnyOrder(
							"D2D53F1A4D184C9BA459D6444152E0231525644000",
							"BD651DDD391145F9B78C2980B2D547F7", "E6BBAC422A4C48F793E75FBBF9CAEE35"
					);
		});

		String responseString2 = when()
				.post("/pull")
				.then().assertThat()
				.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response2;
		try (var jsonReader = Json.createReader(new StringReader(responseString2))) {
			response2 = jsonReader.readObject();
		}

		var itemIds = response2.getJsonArray("items").stream()
				.map(JsonValue::asJsonObject)
				.map(jsonObject -> jsonObject.getString("id"))
				.collect(toList());
		assertThat(itemIds)
				.containsExactlyInAnyOrder(
						"801244036F944E7D808F5F157EED93B0", "CE18D30F61E44C2FA6C1F6FA8024E407",
						"12B7DA5E9EC146B493056384EA89E55D", "AA56D30F61E44C2FA6C1F6FA8024A001"
				);

	}

	@Test
	void deleteItemAndTag() {
		// GIVEN a sync request object
		var responseString = given()
				.body(Json.createObjectBuilder()
						.add("last_sync_ts", 2000000000)
						.add("changes", Json.createObjectBuilder()
								.add("deletions", Json.createArrayBuilder()
										.add(Json.createObjectBuilder()
												.add("sync_id", "801244036F944E7D808F5F157EED93B0")
												.add("ts", 2000000000)
												.add("entity_type", "i")
												.build())
										.add(Json.createObjectBuilder()
												.add("sync_id", "9B6204D188F6489799876DAB20539864")
												.add("ts", 2000000000)
												.add("entity_type", "t")
												.build())
										.build())
								.add("tags", JsonValue.EMPTY_JSON_ARRAY)
								.add("items", JsonValue.EMPTY_JSON_ARRAY)
								.build())
						.build().toString())
				// WHEN performing the request
				.when()
				.post("/sync")
				.then().assertThat()
				.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response;
		try (var jsonReader = Json.createReader(new StringReader(responseString))) {
			response = jsonReader.readObject();
		}

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(response.get("success").toString()).isEqualTo("true");

			var itemIds = response.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds).isEmpty();

			var tagIds = response.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds).isEmpty();

			var deletionIds = response.getJsonArray("deletions_to_add").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds).isEmpty();
		});

		String responseString2 = when()
				.post("/pull")
				.then().assertThat()
				.statusCode(HTTP_OK)
				.extract().body().asString();

		JsonObject response2;
		try (var jsonReader = Json.createReader(new StringReader(responseString2))) {
			response2 = jsonReader.readObject();
		}

		SoftAssertions.assertSoftly(softly -> {
			// AND all the previously existing items are returned in the response
			var itemIds = response2.getJsonArray("items").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(itemIds)
					.containsExactlyInAnyOrder(
							"CE18D30F61E44C2FA6C1F6FA8024E407", "12B7DA5E9EC146B493056384EA89E55D"
					);

			// AND all the previously existing tags are returned in the response
			var tagIds = response2.getJsonArray("tags").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("id"))
					.collect(toList());
			softly.assertThat(tagIds)
					.containsExactlyInAnyOrder("5D1204D188F6489799876DAB2053978E");

			// AND all the previously existing deletions are returned in the response
			var deletionIds = response2.getJsonArray("deletions").stream()
					.map(JsonValue::asJsonObject)
					.map(jsonObject -> jsonObject.getString("sync_id"))
					.collect(toList());
			softly.assertThat(deletionIds)
					.containsExactlyInAnyOrder(
							"D2D53F1A4D184C9BA459D6444152E0231533592800", "D2D53F1A4D184C9BA459D6444152E0231525644000",
							"BD651DDD391145F9B78C2980B2D547F7", "E6BBAC422A4C48F793E75FBBF9CAEE35",
							"801244036F944E7D808F5F157EED93B0", "9B6204D188F6489799876DAB20539864"
					);
		});
	}
}
