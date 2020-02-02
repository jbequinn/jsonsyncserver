package com.jbequinn.jsonsyncserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import io.quarkus.jackson.ObjectMapperCustomizer;

import javax.inject.Singleton;

@Singleton
public class RegisterJSR353ModuleCustomizer implements ObjectMapperCustomizer {
	public void customize(ObjectMapper mapper) {
		mapper.registerModule(new JSR353Module());
	}
}
