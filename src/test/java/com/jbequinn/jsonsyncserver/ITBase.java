package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.Random.class)
public abstract class ITBase {
	static {
		GenericContainer mongoContainer = new GenericContainer("mongo:4.2.3")
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
	protected int port;

	protected RequestSpecification spec = new RequestSpecBuilder()
			.setBaseUri("https://0.0.0.0")
			.addQueryParam("key", "ABCDEF")
			.build();

	@PostConstruct
	public void setUpRestAssured() {
		spec.port(port);

		RestAssured.config = config()
				.logConfig(logConfig()
						.enableLoggingOfRequestAndResponseIfValidationFails())
				.objectMapperConfig(objectMapperConfig()
						.jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
				.sslConfig(sslConfig()
						.relaxedHTTPSValidation()
						.allowAllHostnames());
	}
}
