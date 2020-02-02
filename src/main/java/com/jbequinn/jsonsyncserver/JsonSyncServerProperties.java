package com.jbequinn.jsonsyncserver;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Validated
@Data
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
