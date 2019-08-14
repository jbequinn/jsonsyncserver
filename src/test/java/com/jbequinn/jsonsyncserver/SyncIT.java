package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;

import java.time.Instant;

import static io.restassured.RestAssured.when;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SyncIT {
	private GenericContainer mongoContainer;

	@Inject
	private
	EmbeddedServer server;

	@Inject
	private ObjectMapper objectMapper;

	@BeforeAll
	void setUp() {
		mongoContainer = new GenericContainer("mongo:4.0.10")
				.withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
				.withEnv("MONGO_INITDB_ROOT_PASSWORD", "mypassword")
				.withExposedPorts(27017);

		mongoContainer.start();

		System.setProperty("application.mongo-host", mongoContainer.getContainerIpAddress());
		System.setProperty("application.mongo-port", mongoContainer.getFirstMappedPort() + "");
		System.setProperty("application.mongo-database", "everdo");
		System.setProperty("application.mongo-username", "root");
		System.setProperty("application.mongo-password", "mypassword");
	}

	@AfterAll
	void tearDown() {
		mongoContainer.stop();
	}

	@BeforeEach
	public void setUpRestAssured() {
		RestAssured.baseURI = "https://localhost";
		RestAssured.basePath = "/";
		RestAssured.port = server.getPort();
		RestAssured.requestSpecification = new RequestSpecBuilder()
				.addQueryParam("key", "ABCDEF")
				.build();
		RestAssured.config = config()
				.objectMapperConfig(config().getObjectMapperConfig()
						.jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
				.sslConfig(sslConfig()
						.relaxedHTTPSValidation()
						.allowAllHostnames());
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
}
