package com.irinfosys.websocket.multi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstanceController {

  private final Environment environment;

  @GetMapping("/api/instance")
  public InstanceInfo getInstance() {
    return new InstanceInfo(
        environment.getProperty(
            "app.instance-id",
            "local"
        ),
        environment.getProperty(
            "local.server.port",
            environment.getProperty(
                "server.port",
                "8080"
            )
        )
    );
  }

  public record InstanceInfo(
      String instanceId,
      String port
  ) {
  }
}
