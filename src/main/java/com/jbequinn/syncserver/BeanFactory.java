package com.jbequinn.syncserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbequinn.syncserver.infrastructure.repository.MongoRepository;
import com.jbequinn.syncserver.infrastructure.service.JsonObjectMerger;
import com.jbequinn.syncserver.infrastructure.service.JsonSyncServerService;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;
import java.util.List;

@Factory
public class BeanFactory {

	@Singleton
	ObjectMapper objectMapper() {
		return new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Singleton
	JsonObjectMerger merger() {
		return new JsonObjectMerger();
	}

	@Singleton
	public MongoClient mongoClient(JsonSyncServerProperties properties) {
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

	@Singleton
	MongoRepository repository(ObjectMapper objectMapper, MongoClient mongoClient) {
		return new MongoRepository(mongoClient, objectMapper);
	}

	@Singleton
	JsonSyncServerService service(MongoRepository repository, JsonObjectMerger merger) {
		return new JsonSyncServerService(repository, merger);
	}
}
