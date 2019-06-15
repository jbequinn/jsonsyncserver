package com.jbequinn.jsonsyncserver;

import com.jbequinn.jsonsyncserver.infrastructure.service.JsonObjectMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonValue;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeTest {
	private JsonObjectMerger merger;

	@BeforeEach
	void setUp() {
		merger = new JsonObjectMerger();
	}

	@Test
	void mergeItem() {
		// GIVEN two items
		var one = Json.createObjectBuilder()
				.add("changed_ts", 1L)

				.add("title", "title one")
				.add("title_ts", 1L)

				.add("note", "note")
				.add("note_ts", 1L)

				.add("start_date", 1L)
				.add("start_date_ts", 1L)

				.add("tags", Json.createArrayBuilder()
						.add(Json.createObjectBuilder().add("title", "tag1").build())
						.build())
				.add("tags_changed_ts", 1L)
				.build();
		var two = Json.createObjectBuilder()
				.add("changed_ts", 3L)

				.add("title", "title two")
				.add("title_ts", 2L)

				.add("due_date", 1L)
				.add("due_date_ts", 1L)

				.add("note", "note")
				.add("note_ts", 1L)

				.add("start_date", 0L)
				.add("start_date_ts", 0L)

				.add("tags", Json.createArrayBuilder()
						.add(Json.createObjectBuilder().add("title", "tag2").build())
						.build())

				.add("tags_changed_ts", 3L)
				.build();

		// WHEN those items are merged
		var result = merger.mergeItem(one, two);

		// THEN the result contains the newest element of each item
		assertThat(result.getJsonNumber("changed_ts").longValue()).isEqualTo(3L);

		assertThat(result.getString("title")).isEqualTo("title two");
		assertThat(result.getJsonNumber("title_ts").longValue()).isEqualTo(2L);

		assertThat(result.getJsonNumber("due_date").longValue()).isEqualTo(1L);
		assertThat(result.getJsonNumber("due_date_ts").longValue()).isEqualTo(1L);

		assertThat(result.getString("note")).isEqualTo("note");
		assertThat(result.getJsonNumber("note_ts").longValue()).isEqualTo(1L);

		assertThat(result.getJsonNumber("start_date").longValue()).isEqualTo(1L);
		assertThat(result.getJsonNumber("start_date_ts").longValue()).isEqualTo(1L);

		assertThat(result.getJsonArray("tags").get(0).asJsonObject().getString("title")).isEqualTo("tag2");
		assertThat(result.getJsonNumber("tags_changed_ts").longValue()).isEqualTo(3L);
	}

	@Test
	void mergeTag() {
		// GIVEN two tags
		var one = Json.createObjectBuilder()
				.add("changed_ts", 1L)
				.add("title", "title one")
				.add("color", "something")
				.build();
		var two = Json.createObjectBuilder()
				.add("changed_ts", 2L)
				.add("title", "title two")
				.build();

		// WHEN those tags are merged
		var result = merger.mergeTag(one, two);

		// THEN the result is the most recent tag
		assertThat(result.getJsonNumber("changed_ts").longValue()).isEqualTo(2L);

		assertThat(result.getString("title")).isEqualTo("title two");
		assertThat(result.get("color")).isNull();
	}
}
