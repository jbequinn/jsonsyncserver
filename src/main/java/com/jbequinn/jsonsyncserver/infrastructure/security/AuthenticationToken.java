package com.jbequinn.jsonsyncserver.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class AuthenticationToken extends AbstractAuthenticationToken {
  private Object credentials;

  public AuthenticationToken(Object credentials) {
    super(null);
    this.credentials = credentials;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }
}
