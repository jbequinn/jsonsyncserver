package com.jbequinn.jsonsyncserver.filter;

import com.jbequinn.jsonsyncserver.JsonSyncServerProperties;
import io.quarkus.security.AuthenticationFailedException;
import lombok.extern.flogger.Flogger;
import org.apache.http.auth.AuthenticationException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Flogger
@Provider
public class ParameterAuthenticationFilter implements ContainerRequestFilter {

	@Inject
	private JsonSyncServerProperties properties;

	@Context
	private HttpServletRequest servletRequest;

	@Override
	public void filter(ContainerRequestContext requestContext) {
    /*
    authentication tokens should *NEVER* be passed in query parameters. see:
    https://tools.ietf.org/html/rfc6750#section-5.3
     */

		log.atFine().log("Authenticating key");
		if (!properties.getKey().equals(servletRequest.getParameter("key"))) {
			log.atFine().log("Wrong authentication key");
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		} else {
			log.atFine().log("Authentication sucessful");
		}
	}
}
