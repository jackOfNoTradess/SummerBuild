package com.example.SummerBuild.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public abstract class EventsDtoTestMixin {

  // Override the read-only getter with AUTO access (to allow deserialization in tests)
  @JsonProperty(access = JsonProperty.Access.AUTO)
  public abstract UUID getId();

  @JsonProperty(access = JsonProperty.Access.AUTO)
  public abstract UUID getHostUuid();
}
