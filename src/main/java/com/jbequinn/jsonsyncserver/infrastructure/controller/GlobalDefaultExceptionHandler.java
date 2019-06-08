package com.jbequinn.jsonsyncserver.infrastructure.controller;

import com.jbequinn.jsonsyncserver.domain.model.OptimisticLockingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.FileNotFoundException;

@ControllerAdvice
public class GlobalDefaultExceptionHandler {
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(FileNotFoundException.class)
  public void handleFileNotFound() {
    // Nothing to do
  }
	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler(OptimisticLockingException.class)
	public void handleOptimisticLocking() {
		// Nothing to do
	}
}
