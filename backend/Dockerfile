# Usado só pelo Render para montar o back-end na homologação (SCRUM-120).
# Ninguém precisa de Docker na máquina: localmente o back-end roda com mvn spring-boot:run.

# 1) Compila o projeto com Maven e Java 21. Os testes já rodam no CI antes do deploy.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# 2) Imagem final, só com o Java 21 e o .jar gerado.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/filasus-*.jar app.jar

# O Render informa a porta pela variável PORT (server.port=${PORT:8080}).
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
