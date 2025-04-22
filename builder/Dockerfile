# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the Gradle wrapper executable
COPY gradlew .
# Copy the Gradle wrapper configuration directory
COPY gradle ./gradle

# Copy the main Gradle build script and settings
# Add any other necessary configuration files if they exist at the root (e.g., gradle.properties)
COPY build.gradle .
COPY settings.gradle .

# Optional: Download dependencies first to leverage Docker cache
# This assumes the build files don't depend on source code being present yet.
# Uncomment the following line if you want to cache dependencies separately.
# RUN ./gradlew dependencies --no-daemon

# Copy the rest of the project source code
COPY . .

# Grant execution permission to the Gradle wrapper
RUN chmod +x ./gradlew

# Set the default command to assemble the project artifacts
CMD ["./gradlew", "assemble"]
