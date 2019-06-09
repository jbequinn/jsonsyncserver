package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.jsonsyncserver.infrastructure.service.JsonSyncServerService;
import lombok.extern.flogger.Flogger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.json.JsonObject;
import java.time.Instant;

import static org.springframework.web.servlet.function.ServerResponse.ok;

@Component
@Flogger
public class JsonSyncServerHandler {
	private final JsonSyncServerService jsonSyncServerService;
	private final ObjectMapper objectMapper;

	public JsonSyncServerHandler(JsonSyncServerService jsonSyncServerService, ObjectMapper objectMapper) {
		this.jsonSyncServerService = jsonSyncServerService;
		this.objectMapper = objectMapper;
	}

	ServerResponse handlePushFile(ServerRequest serverRequest) throws Exception {
		log.atFinest()
				.log("Push invoked: %s", serverRequest.servletRequest().getRequestURL().append('?')
						.append(serverRequest.servletRequest().getQueryString()));

		jsonSyncServerService.saveAllItemsTagsAndDeletions(
				objectMapper.readValue(serverRequest.servletRequest().getInputStream(), JsonObject.class)
		);

		return ok().build();
	}

	ServerResponse handlePullFile(ServerRequest serverRequest) throws Exception {
		log.atFinest()
				.log("Pull invoked");

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(objectMapper.writeValueAsString(jsonSyncServerService.findAllItemsTagsAndDeletions()));
	}

	ServerResponse handleGetTime(ServerRequest serverRequest) {
		log.atFinest()
				.log("Time invoked: %s", serverRequest.servletRequest().getRequestURL().append('?')
						.append(serverRequest.servletRequest().getQueryString()));

		long epochMilli = Instant.now().toEpochMilli();
		log.atFinest().log("current time: %s ms", epochMilli);
		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"server_time_ms\": " + epochMilli + " }");
	}

	ServerResponse handleSyncJson(ServerRequest serverRequest) throws Exception {
		log.atFinest()
				.log("Sync invoked: %s", serverRequest.servletRequest().getRequestURL().append('?')
						.append(serverRequest.servletRequest().getQueryString()));

		return ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(
						objectMapper.writeValueAsString(jsonSyncServerService.sync(
								objectMapper.readValue(serverRequest.servletRequest().getInputStream(),
										JsonObject.class
								)
						)
				));
	}

	ServerResponse handleWipe(ServerRequest serverRequest) {
		log.atFinest()
				.log("Wipe invoked: %s", serverRequest.servletRequest().getRequestURL().append('?')
						.append(serverRequest.servletRequest().getQueryString()));

		jsonSyncServerService.deleteAllItemsTagsAndDeletions();

		return ok().build();
	}
}
