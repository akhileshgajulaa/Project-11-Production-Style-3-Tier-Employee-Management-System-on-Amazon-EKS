# ---------------------------------------------------------------------------
# Stage 1 — Build the Spring Boot application with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only the POM first so dependency resolution is cached separately
# from source-code changes (faster rebuilds).
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 — Run the application on a minimal JRE, as a non-root user
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd --system --uid 1001 emsuser

COPY --from=build /app/target/employee-management-system-1.0.0.jar app.jar

RUN chown -R emsuser:emsuser /app
USER 1001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
