package com.jbequinn.jsonsyncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
public class JsonSyncServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(JsonSyncServerApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> routes(JsonSyncServerHandler handler) {
    return route()
      .POST("/push", handler::handlePushFile)
      .POST("/pull", handler::handlePullFile)
      .GET("/time", handler::handleGetTime)
      .POST("/sync", handler::handleSyncJson)
      .POST("/wipe", handler::handleWipe)
      .filter((serverRequest, handlerFunction) -> handlerFunction.handle(serverRequest))
      .build();
  }
}
