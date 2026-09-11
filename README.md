# ordermod

Aplicação de pedidos em Spring Boot, Spring Modulith e PostgreSQL.

## Ambiente local

Requer JDK 25 e Docker Compose.

```bash
docker compose up -d
./mvnw spring-boot:run
```

- Grafana: http://localhost:3000/ (usuário e senha padrão: `admin`)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- PostgreSQL: `localhost:5433`, banco `ordermod`

O dashboard **Ordermod / PostgreSQL Overview** é provisionado automaticamente. Consulte
[Observabilidade do PostgreSQL](docs/observabilidade-postgresql.md) para arquitetura, métricas,
segurança e diagnóstico.
