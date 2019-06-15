package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.SocketSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class JsonSyncServerConfiguration {
	private final JsonSyncServerProperties properties;

	public JsonSyncServerConfiguration(JsonSyncServerProperties properties) {
		this.properties = properties;
	}

	@Bean
	public JSR353Module jsonModule() {
		return new JSR353Module();
	}

	@Bean
	public MongoClient mongoClient() {
		return MongoClients.create(
				MongoClientSettings.builder()
						.readPreference(ReadPreference.primaryPreferred())
						.applyToSocketSettings(builder -> builder
								.connectTimeout(60, TimeUnit.MINUTES)
								.readTimeout(120, TimeUnit.SECONDS)
						)
						.applyToClusterSettings(builder ->
								builder.hosts(List.of(new ServerAddress(
										properties.getMongoHost(),
										properties.getMongoPort()
								)))
						)
						.credential(MongoCredential.createCredential(
								properties.getMongoUsername(),
								"everdo",
								properties.getMongoPassword().toCharArray()
								)
						)
						.build());
	}
}
