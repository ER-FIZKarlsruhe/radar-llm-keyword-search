# Packages the jar built by Maven into a runtime image. Build the jar first:
#   mvn package
#   docker build -t radar-llm-keyword-search .
#   docker run -d -p 8080:8080 radar-llm-keyword-search
FROM eclipse-temurin:21-jre

RUN useradd --system --no-create-home appuser
WORKDIR /app
COPY target/radar-llm-keyword-search-*.jar app.jar
USER appuser

EXPOSE 8080

# Configuration via environment variables (Spring relaxed binding), e.g.
#   RADAR_HTTP_PROXY_HOST=           (empty = no proxy for Ollama, GND and TIB)
#   RADAR_HTTP_PROXY_PORT=8888
#   RADAR_OLLAMA_BASE_URL=...
#   RADAR_OLLAMA_MODELS=keyword-extractor,keyword-extractor1,keyword-extractor2
#   RADAR_TIB_ONTOLOGIES=chebi,gnd
# JAVA_TOOL_OPTIONS can be used for JVM settings, e.g. -XX:MaxRAMPercentage=75.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
