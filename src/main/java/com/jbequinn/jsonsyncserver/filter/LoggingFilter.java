package com.jbequinn.jsonsyncserver.filter;

import lombok.extern.flogger.Flogger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Flogger
@Provider
public class LoggingFilter implements ContainerRequestFilter {

	@Context
	UriInfo info;

	@Override
	public void filter(ContainerRequestContext context) {

		final String method = context.getMethod();
		final String path = info.getPath();

		log.atFine().log("Request to method: %s. Path: %s", method, path);
	}
}
