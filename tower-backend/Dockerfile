FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk upgrade && apk add --no-cache bash curl
WORKDIR /tower
COPY build/libs/tower-backend-*.jar /tower/tower-backend-all.jar
COPY src/main/jib/tower.sh /usr/local/bin/tower.sh
RUN chmod +x /usr/local/bin/tower.sh 2>/dev/null || true
EXPOSE 8080
ENV MICRONAUT_HOST=0.0.0.0
CMD ["java", "-XX:TieredStopAtLevel=1", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "-jar", "/tower/tower-backend-all.jar"]
