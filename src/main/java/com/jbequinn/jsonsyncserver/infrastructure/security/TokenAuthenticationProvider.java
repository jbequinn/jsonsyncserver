package com.jbequinn.jsonsyncserver.infrastructure.security;

import com.jbequinn.jsonsyncserver.JsonSyncServerProperties;
import lombok.extern.flogger.Flogger;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;

@Flogger
public class TokenAuthenticationProvider implements AuthenticationProvider {
  private final JsonSyncServerProperties properties;

  public TokenAuthenticationProvider(JsonSyncServerProperties properties) {
    this.properties = properties;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String key = Optional.ofNullable(authentication.getCredentials())
      .map(Object::toString)
      .orElse(null);

    log.atFine().log("Authenticating key");
    if (!properties.getKey().equals(key)) {
			log.atFine().log("Wrong authentication key");
      return null;
    } else {
			log.atFine().log("Authentication sucessful");
      return authentication;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return true;
  }
}
