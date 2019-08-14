package com.jbequinn.syncserver;

import io.micronaut.context.annotation.ConfigurationProperties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("application")
public class JsonSyncServerProperties {
	@NotBlank
	private String key;
	@NotEmpty
	private String mongoHost = "db";
	private int mongoPort = 27017;
	@NotEmpty
	private String mongoUsername;
	@NotEmpty
	private String mongoPassword;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getMongoHost() {
		return mongoHost;
	}

	public void setMongoHost(String mongoHost) {
		this.mongoHost = mongoHost;
	}

	public int getMongoPort() {
		return mongoPort;
	}

	public void setMongoPort(int mongoPort) {
		this.mongoPort = mongoPort;
	}

	public String getMongoUsername() {
		return mongoUsername;
	}

	public void setMongoUsername(String mongoUsername) {
		this.mongoUsername = mongoUsername;
	}

	public String getMongoPassword() {
		return mongoPassword;
	}

	public void setMongoPassword(String mongoPassword) {
		this.mongoPassword = mongoPassword;
	}
}
