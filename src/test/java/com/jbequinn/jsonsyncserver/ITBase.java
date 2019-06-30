package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
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
		GenericContainer mongoContainer = new GenericContainer("mongo:4.0.10")
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
			.setBaseUri("https://localhost")
			.setBasePath("/")
			.addQueryParam("key", "ABCDEF")
			.addFilter(new ResponseLoggingFilter())
			.addFilter(new RequestLoggingFilter())
			.build();

	@BeforeEach
	public void setUpRestAssured() {
		spec.port(port);

		RestAssured.config = config()
				.objectMapperConfig(config().getObjectMapperConfig()
						.jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
				.sslConfig(sslConfig()
						.relaxedHTTPSValidation()
						.allowAllHostnames());
	}
}
