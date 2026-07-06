FROM maven:3.8.5-openjdk-26 AS build
WORKDIR /app
COPY . /app/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:26-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
