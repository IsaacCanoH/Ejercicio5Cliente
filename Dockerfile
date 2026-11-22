FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/main/ src/main/

RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -Dmaven.test.skip=true package


FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --create-home app

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

ENV SERVER_PORT=8082

USER app

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
