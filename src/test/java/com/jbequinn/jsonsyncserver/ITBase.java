package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.testcontainers.containers.GenericContainer;

import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ITBase {
	static {
		GenericContainer mongoContainer = new GenericContainer("mongo:4.1.13")
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

	@Autowired
	private ObjectMapper objectMapper;
	@LocalServerPort
	private int port;

	@BeforeEach
	public void setUpRestAssured() {
		RestAssured.baseURI = "https://localhost";
		RestAssured.basePath = "/";
		RestAssured.port = port;
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
}
