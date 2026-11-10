FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV TZ=Asia/Ho_Chi_Minh
EXPOSE 8080
ENTRYPOINT ["java","-Xmx300m","-XX:+UseSerialGC","-Duser.timezone=Asia/Ho_Chi_Minh","-jar","app.jar"]