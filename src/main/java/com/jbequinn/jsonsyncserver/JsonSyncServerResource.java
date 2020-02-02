package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.jsonsyncserver.service.JsonSyncServerService;
import lombok.extern.flogger.Flogger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;

@Flogger
@Path("/")
public class JsonSyncServerResource {

	private final JsonSyncServerService service;
	private final ObjectMapper objectMapper;

	public JsonSyncServerResource(JsonSyncServerService service, ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@GET
	@Path("/time")
	@Produces(MediaType.APPLICATION_JSON)
	public Response time(@Context UriInfo ui) {
		log.atFinest()
				.log("Time invoked: %s?%s", ui.getAbsolutePath(), ui.getRequestUri());

		long epochMilli = Instant.now().toEpochMilli();
		log.atFinest().log("current time: %s ms", epochMilli);
		return Response
				.ok("{\"server_time_ms\": " + epochMilli + " }")
				.build();
	}

	@POST
	@Path("/pull")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pull() throws JsonProcessingException {
		log.atFinest()
				.log("Pull invoked");

		return Response
				.ok(objectMapper.writeValueAsString(service.findAllItemsTagsAndDeletions()))
				.build();
	}

	@POST
	@Path("/push")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response push(@Context UriInfo ui, String body) throws IOException {
		log.atFinest()
				.log("Push invoked: %s?%s", ui.getAbsolutePath(), ui.getRequestUri());

		/*
		JsonReader jsonReader = Json.createReader(new StringReader(body));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		 */

		service.saveAllItemsTagsAndDeletions(objectMapper.readValue(body, JsonObject.class));

		return Response.ok().build();
	}

	@POST
	@Path("/sync")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response sync(@Context UriInfo ui, String body) throws Exception {
		log.atFinest()
				.log("Sync invoked: %s?%s", ui.getAbsolutePath(), ui.getRequestUri());

		return Response
				.ok(objectMapper.writeValueAsString(service.sync(objectMapper.readValue(body, JsonObject.class))))
				.build();
	}

	@POST
	@Path("/wipe")
	public Response wipe(@Context UriInfo ui) throws Exception {
		log.atFinest()
				.log("Wipe invoked: %s?%s", ui.getAbsolutePath(), ui.getRequestUri());

		service.deleteAllItemsTagsAndDeletions();

		return Response.ok().build();
	}
}
