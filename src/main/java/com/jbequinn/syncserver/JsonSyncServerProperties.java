package com.jbequinn.syncserver;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
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
}
