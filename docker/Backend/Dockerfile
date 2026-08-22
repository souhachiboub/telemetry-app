# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier d'abord le pom.xml pour profiter du cache Docker sur les dépendances
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copier le reste des sources et builder le jar (tests sautés pour un build rapide)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
