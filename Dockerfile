# --------------------------------------------------
# Stage 1 - Build
# --------------------------------------------------
FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom first for better layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# --------------------------------------------------
# Stage 2 - Runtime
# --------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN useradd -m spring

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]


# Importing JDK and copying required files
#FROM openjdk:17-jdk AS build
#FROM eclipse-temurin:17-jre AS build
#WORKDIR /app
#COPY pom.xml .
#COPY src src
#
## Copy Maven wrapper
#COPY mvnw .
#COPY .mvn .mvn
#
## Set execution permission for the Maven wrapper
#RUN chmod +x ./mvnw
#RUN ./mvnw clean package -DskipTests
#
## Stage 2: Create the final Docker image using OpenJDK 19
#FROM openjdk:17-jdk
#VOLUME /tmp
#
## Copy the JAR from the build stage
#COPY --from=build /app/target/*.jar app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]
#EXPOSE 8080