#!/bin/sh

BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -r "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper JAR not found: $WRAPPER_JAR" >&2
  echo "Restore .mvn/wrapper/maven-wrapper.jar before running ./mvnw." >&2
  exit 1
fi

exec java \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -classpath "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
