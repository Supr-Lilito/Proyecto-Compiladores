# Use a Maven image with Eclipse Temurin 21 on Alpine Linux as the build stage.
# This provides Maven and Java for building the project.
FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container.
# All subsequent commands will be executed relative to this directory.
WORKDIR /app

# Copy the Maven project file (pom.xml) into the container.
# This is done early to leverage Docker's layer caching for dependencies.
COPY pom.xml .

# Download all project dependencies.
# This command populates the local Maven repository, making subsequent builds faster.
RUN mvn dependency:go-offline

# Copy the source code into the container.
COPY src ./src

# Package the application into a JAR file.
# -DskipTests is used to skip running tests during the build, which can be done in a separate CI step.
# The resulting JAR will be located in the 'target/' directory.
RUN mvn package -DskipTests
