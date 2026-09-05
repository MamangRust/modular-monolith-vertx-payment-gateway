# ── vertx-payment-gateway Justfile ─────────────────────────────────────────
# Task-runner shortcuts for the Maven multi-module build.
# Usage: just <recipe> [args]
# Install just: https://github.com/casey/just

set shell := ["bash", "-uc"]

# List all available recipes
default:
    @just --list

# ── Test ────────────────────────────────────────────────────────────────────

# Run tests for all modules
test:
    mvn -B test -Dsurefire.failIfNoSpecifiedTests=false

# Run tests for a single module: `just test-module common`
test-module module:
    mvn -B test -pl {{ module }} -Dsurefire.failIfNoSpecifiedTests=false

# Run a single test class: `just test-class common KafkaServiceTest`
test-class module class:
    mvn -B test -pl {{ module }} -Dtest={{ class }} -Dsurefire.failIfNoSpecifiedTests=false

# ── Build ───────────────────────────────────────────────────────────────────

# Clean + compile + test + package (same as CI pipeline)
verify:
    mvn -B clean verify -Dsurefire.failIfNoSpecifiedTests=false

# Compile all modules (no tests)
compile:
    mvn -B compile

# Compile a module + its dependencies: `just compile-module auth`
compile-module module:
    mvn -B compile -pl {{ module }} -am

# Clean build without running tests
package:
    mvn -B clean package -DskipTests

# ── Misc ────────────────────────────────────────────────────────────────────

# Delete all build outputs (target/ directories)
clean:
    mvn -B clean
