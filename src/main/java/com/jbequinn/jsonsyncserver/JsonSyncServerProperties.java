package com.jbequinn.jsonsyncserver;

import io.quarkus.arc.config.ConfigProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@ConfigProperties(prefix = "application")
@Data
public class JsonSyncServerProperties {
  @NotBlank
  private String key;
}
