package io.example.common.chaos;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChaosManager {
  private static final Logger log = LoggerFactory.getLogger(ChaosManager.class);

  private static final String DEFAULT_CONFIG_PATH = "chaos.yaml";

  /** Env var that gates every chaos interceptor. Absent or non-"true" means disabled. */
  public static final String CHAOS_ENABLED_ENV = "CHAOS_ENABLED";

  private final String configPath;
  private final boolean enabled;
  private volatile ChaosConfig currentConfig;
  private long lastModified = 0;

  /**
   * Whether chaos injection is enabled for this process.
   *
   * <p>Defaults to {@code false}: the wrappers are reflective proxies around the SQL pool,
   * Kafka producer and gRPC client, so they must not be installed in production unless
   * explicitly asked for. Set {@code CHAOS_ENABLED=true} in staging/chaos-testing only.
   */
  public static boolean isChaosEnabled() {
    return isChaosEnabled(System.getenv());
  }

  /** Testable overload: same rules, explicit env map. */
  static boolean isChaosEnabled(java.util.Map<String, String> env) {
    String value = env.get(CHAOS_ENABLED_ENV);
    return value != null && Boolean.parseBoolean(value.trim());
  }

  public ChaosManager() {
    this(System.getenv().getOrDefault("CHAOS_CONFIG_PATH", DEFAULT_CONFIG_PATH), isChaosEnabled());
  }

  public ChaosManager(String configPath) {
    this(configPath, isChaosEnabled());
  }

  /** Testable constructor that enables deterministic chaos without process environment mutation. */
  ChaosManager(String configPath, boolean enabled) {
    this.configPath = configPath;
    this.enabled = enabled;
    this.currentConfig = new ChaosConfig();
    if (enabled) {
      loadConfig();
    } else {
      log.info("🧯 Chaos disabled ({} is not \"true\"); skipping config load from {}",
          CHAOS_ENABLED_ENV, configPath);
    }
  }

  public void startWatcher(Vertx vertx) {
    if (!enabled) {
      // No periodic timer when chaos is off: an operator dropping a chaos.yaml on the pod
      // must not silently arm fault injection in production.
      return;
    }
    // Check for file modification every 5 seconds
    vertx.setPeriodic(5000, id -> {
      File file = new File(configPath);
      if (file.exists() && file.lastModified() > lastModified) {
        log.info("🔄 Detect modifications in {}, reloading chaos config...", configPath);
        loadConfig();
      }
    });
  }

  public synchronized void loadConfig() {
    File file = new File(configPath);
    if (!file.exists()) {
      log.warn("⚠️ Chaos config file not found at: {}. Using default empty config.", file.getAbsolutePath());
      this.currentConfig = new ChaosConfig();
      return;
    }

    try (InputStream input = new FileInputStream(file)) {
      LoaderOptions loaderOptions = new LoaderOptions();
      Yaml yaml = new Yaml(new Constructor(ChaosConfig.class, loaderOptions));
      ChaosConfig config = yaml.load(input);
      if (config == null) {
        config = new ChaosConfig();
      }
      this.currentConfig = config;
      this.lastModified = file.lastModified();
      log.info("✅ Chaos configuration successfully loaded/reloaded from {}. Total policies: {}", 
          configPath, config.getPolicies().size());

      // Trigger resource sabotage if any policy is enabled
      for (ChaosPolicy policy : config.getPolicies()) {
        if (policy.isEnabled()) {
          if ("cpu".equalsIgnoreCase(policy.getType())) {
            ChaosResourceSabotage.startCpuPressure(policy.getCpuCores(), policy.getDuration());
          } else if ("memory".equalsIgnoreCase(policy.getType())) {
            ChaosResourceSabotage.startMemoryLeak(policy.getMemoryMb(), policy.getDuration());
          }
        }
      }
    } catch (Exception e) {
      log.error("❌ Failed to load/parse chaos configuration from {}", configPath, e);
    }
  }

  public ChaosConfig getConfig() {
    return currentConfig;
  }

  public List<ChaosPolicy> getPolicies() {
    return currentConfig != null ? currentConfig.getPolicies() : new ArrayList<>();
  }

  public ChaosPolicy evaluate(String type, String target) {
    ChaosConfig cfg = currentConfig;
    if (cfg == null || cfg.getPolicies() == null) return null;

    for (ChaosPolicy policy : cfg.getPolicies()) {
      if (policy.isEnabled() && type.equalsIgnoreCase(policy.getType())) {
        if (matches(target, policy.getTarget())) {
          return policy;
        }
      }
    }
    return null;
  }

  private boolean matches(String target, String pattern) {
    if (pattern == null) return false;
    if ("all".equalsIgnoreCase(pattern) || "*".equals(pattern)) return true;
    if (target == null) return false;

    if (pattern.endsWith("*")) {
      String prefix = pattern.substring(0, pattern.length() - 1);
      return target.startsWith(prefix);
    }

    try {
      return target.equals(pattern) || target.matches(pattern);
    } catch (Exception e) {
      return target.contains(pattern);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void halt() {
    log.info("🛑 Halt command triggered: disabling all chaos policies.");
    ChaosConfig cfg = currentConfig;
    if (cfg != null && cfg.getPolicies() != null) {
      for (ChaosPolicy policy : cfg.getPolicies()) {
        policy.setEnabled(false);
      }
    }
    ChaosResourceSabotage.haltAll();
  }
}
