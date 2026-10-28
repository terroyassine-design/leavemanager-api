# ---- Build stage: compile and package the jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Download dependencies first so this layer is cached when only the code changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# ---- Run stage: small JRE image with only the jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --no-create-home app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
