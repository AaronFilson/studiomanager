# syntax=docker/dockerfile:1

# The API image. Built by .github/workflows/deploy-dev.yml with `api/` as the context; CI has
# already run the tests by then, so this stage only packages.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar \
 && java -Djarmode=tools -jar build/libs/studiomanager-api.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
WORKDIR /app
# Least-changing layers first, so a code change rebuilds only the last one
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# The same JVM flags for the training run and real runs, so the AOT cache stays valid. Serial
# GC is what the JVM would pick anyway on a small Container Apps replica (under 2 vCPUs).
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75"

# Training run: start the application context once, without a database, and record the
# classes it loads and links into an AOT cache (JEP 483/514). Real starts load from the cache,
# which shortens cold starts when Container Apps scales up from zero.
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -Dspring.profiles.active=aot-training \
        -jar studiomanager-api.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "studiomanager-api.jar"]
