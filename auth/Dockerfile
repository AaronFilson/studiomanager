# syntax=docker/dockerfile:1

# The GitHub sign-in bridge's image (ADR 20). Built by .github/workflows/deploy-dev.yml with the
# repo root as the context, because the bridge builds with the API's Gradle wrapper and version
# catalog (api/settings.gradle.kts includes ../auth). Dockerfile.dockerignore keeps the context to
# those files. CI has already run the tests, so this stage only packages.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY api/gradlew api/settings.gradle.kts api/build.gradle.kts api/gradle.properties api/
COPY api/gradle api/gradle
COPY auth/build.gradle.kts auth/
COPY auth/src auth/src
WORKDIR /workspace/api
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :auth:bootJar \
 && java -Djarmode=tools -jar ../auth/build/libs/studiomanager-auth.jar extract --layers --destination /workspace/extracted

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75"

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "studiomanager-auth.jar"]
