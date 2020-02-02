package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
						.applyToClusterSettings(builder ->
								builder.hosts(List.of(new ServerAddress(
										properties.getMongoHost(),
										properties.getMongoPort()
								))))
						.credential(MongoCredential.createCredential(
								properties.getMongoUsername(),
								"admin",
								properties.getMongoPassword().toCharArray()
								))
						.build());
	}
}