FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S whichone && adduser -S whichone -G whichone
WORKDIR /app
RUN mkdir /app/data && chown whichone:whichone /app/data
COPY --from=build /build/target/whichone-1.0.0.jar /app/app.jar
USER whichone
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
