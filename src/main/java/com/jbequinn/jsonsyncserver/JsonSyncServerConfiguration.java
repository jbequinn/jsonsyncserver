package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import com.jbequinn.jsonsyncserver.infrastructure.security.TokenAuthenticationFilter;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties
public class JsonSyncServerConfiguration {

	@Bean
	@ConfigurationProperties(prefix = "application")
	public JsonSyncServerProperties jsonSyncServerProperties() {
		return new JsonSyncServerProperties();
	}

	@Bean
	public JSR353Module jsonModule() {
		return new JSR353Module();
	}

	@Bean
	public TokenAuthenticationFilter tokenAuthenticationFilter() {
		return new TokenAuthenticationFilter();
	}

	@Bean
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
}
