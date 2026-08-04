FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw || true
RUN if [ -f mvnw ]; then ./mvnw clean package -DskipTests; else mvn clean package -DskipTests; fi

EXPOSE 8080

CMD ["java","-jar","target/unlock-backend-0.0.1-SNAPSHOT.jar"]