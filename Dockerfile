# The image of the application: docker build -t adressen-spring .   then   docker run --rm --name adressen-spring -p 8080:8080 adressen-spring
# Written once by `pisa init` because the architecture description has a `docker { }` block; change it as you like,
# `pisa init --force` leaves it alone.

# Build: the jar, without the tests (they run with `mvn test`)
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package

# Run: only the JRE, as a user without rights
FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --home-dir /app app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# The TCP server of H2 is for database tools in development
ENV H2_TCP_ENABLED=false
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
