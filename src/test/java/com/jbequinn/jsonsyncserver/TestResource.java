package com.jbequinn.jsonsyncserver;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;

import java.util.HashMap;
import java.util.Map;

@QuarkusTestResource(TestResource.Initializer.class)
public class TestResource {
	public static class Initializer implements QuarkusTestResourceLifecycleManager {

		private GenericContainer mongoContainer;

		@Override
		public Map<String, String> start() {
			mongoContainer = new GenericContainer("mongo:4.2.3")
					.withExposedPorts(27017)
					.withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
					.withEnv("MONGO_INITDB_ROOT_PASSWORD", "mypassword");
			mongoContainer.start();

			final Map<String, String> systemProps = new HashMap<>();
			systemProps.put("quarkus.mongodb.hosts", mongoContainer.getContainerIpAddress()
					+ ":" + mongoContainer.getMappedPort(27017));

			return systemProps;
		}

		@Override
		public void stop() {
			if (mongoContainer != null) {
				mongoContainer.stop();
			}
		}
	}
}
