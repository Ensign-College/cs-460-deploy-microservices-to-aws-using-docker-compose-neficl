package com.example.explorecalijpa.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Service that exposes boolean feature flags backed by configuration
 * properties.
 */
@Component
public class FeatureFlagService {

  private final Environment environment;

  public FeatureFlagService(Environment environment) {
    this.environment = environment;
  }

  public boolean isEnabled(String featureName) {
    return environment.getProperty("features." + featureName, Boolean.class, false);
  }
}
