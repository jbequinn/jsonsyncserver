package com.jbequinn.jsonsyncserver.infrastructure.security;

import com.jbequinn.jsonsyncserver.JsonSyncServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Component
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  private final JsonSyncServerProperties properties;
  private final TokenAuthenticationFilter tokenAuthenticationFilter;

  public SecurityConfig(JsonSyncServerProperties properties, TokenAuthenticationFilter tokenAuthenticationFilter) {
    this.properties = properties;
		this.tokenAuthenticationFilter = tokenAuthenticationFilter;
	}

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(new TokenAuthenticationProvider(properties));
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .authorizeRequests().anyRequest().authenticated()
      .and()
      .sessionManagement().sessionCreationPolicy(STATELESS)
      .and()
      .exceptionHandling()
      .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
      .and()
      .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  }
}
