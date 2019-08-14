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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.IOException;
import java.time.Instant;

import static io.micronaut.http.HttpResponse.ok;

@Controller
public class SyncController {
	private Logger logger = LoggerFactory.getLogger(SyncController.class);

	private final JsonSyncServerService service;
	private final ObjectMapper objectMapper;

	public SyncController(JsonSyncServerService service, ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@Get(uri = "/time", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> time(HttpRequest<?> request) {
		logger.debug("Time invoked: {}", request.getPath());

		long epochMilli = Instant.now().toEpochMilli();
		logger.debug("current time: {} ms", epochMilli);
		return ok("{\"server_time_ms\": " + epochMilli + " }");
	}

	@Post(uri = "/pull", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> pull() throws JsonProcessingException {
		logger.debug("Pull invoked");

		return ok(objectMapper.writeValueAsString(service.findAllItemsTagsAndDeletions()));
	}

	@Post(uri = "/push", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> push(HttpRequest<?> request, @Body String body) throws IOException {
		logger.debug("Push invoked: {}", request.getPath());

		service.saveAllItemsTagsAndDeletions(objectMapper.readValue(body, JsonObject.class));

		return ok();
	}

	@Post(uri = "/sync", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> sync(HttpRequest<?> request, @Body String body) throws Exception {
		logger.debug("Sync invoked: {}", request.getPath());

		return ok(objectMapper.writeValueAsString(service.sync(objectMapper.readValue(body, JsonObject.class))));
	}

	@Post(uri = "/wipe", produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> wipe(HttpRequest<?> request) throws Exception {
		logger.debug("Wipe invoked: {}", request.getPath());

		service.deleteAllItemsTagsAndDeletions();

		return ok();
	}
}
