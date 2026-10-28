# FilaSUS - back-end

Spring Boot 3 (Java 21, Maven) com Spring Web, Security, Data JPA, Validation, Actuator e PostgreSQL.

    mvn spring-boot:run                          # sobe a API em http://localhost:8080
    mvn verify                                   # compila e roda os testes (H2 em memória; no CI, PostgreSQL)

Health check: http://localhost:8080/actuator/health
Variáveis: DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET (padrões em src/main/resources/application.properties).
