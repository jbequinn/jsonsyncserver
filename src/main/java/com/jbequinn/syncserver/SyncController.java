package com.jbequinn.syncserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.syncserver.infrastructure.service.JsonSyncServerService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.flogger.Flogger;

import javax.json.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static io.micronaut.http.HttpResponse.ok;

@Controller
@Flogger
public class SyncController {
	private final JsonSyncServerService service;
	private final ObjectMapper objectMapper;

	public SyncController(JsonSyncServerService service, ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@Get(uri = "/time", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> time(HttpRequest<?> request) {
		log.atFinest().log("Time invoked: %s", request.getPath());

		long epochMilli = Instant.now().toEpochMilli();
		log.atFinest().log("current time: %s ms", epochMilli);
		return ok("{\"server_time_ms\": " + epochMilli + " }");
	}

	@Post(uri = "/pull", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> pull() throws JsonProcessingException {
		log.atFinest().log("Pull invoked");

		return ok(objectMapper.writeValueAsString(service.findAllItemsTagsAndDeletions()));
	}

	@Post(uri = "/push", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> push(HttpRequest<?> request, @Body String body) throws IOException {
		log.atFinest().log("Push invoked: %s", request.getPath());

		service.saveAllItemsTagsAndDeletions(objectMapper.readValue(body, JsonObject.class));

		return ok();
	}

	@Post(uri = "/sync", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> sync(HttpRequest<?> request, @Body String body) throws Exception {
		log.atFinest().log("Sync invoked: %s", request.getPath());

		return ok(objectMapper.writeValueAsString(service.sync(objectMapper.readValue(body, JsonObject.class))));
	}

	@Post(uri = "/wipe", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> wipe(HttpRequest<?> request) throws Exception {
		log.atFinest().log("Wipe invoked: %s", request.getPath());

		service.deleteAllItemsTagsAndDeletions();

		return ok();
	}
}
