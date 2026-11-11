FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system signaltrellis \
    && useradd --system --gid signaltrellis --home-dir /var/lib/signaltrellis signaltrellis \
    && mkdir -p /var/lib/signaltrellis/documents \
    && chown -R signaltrellis:signaltrellis /var/lib/signaltrellis
WORKDIR /app
COPY --from=build /workspace/target/signaltrellis-0.1.0-SNAPSHOT.jar /app/app.jar
USER signaltrellis
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
