# Observabilidade do PostgreSQL

O ambiente local coleta três perspectivas complementares:

- métricas internas do PostgreSQL, incluindo conexões, transações, cache, locks, tabelas e `pg_stat_statements`;
- spans e métricas JDBC produzidos pela aplicação, com comandos SQL sanitizados e sem valores de parâmetros;
- CPU, memória, rede, disco, processos e disponibilidade do contêiner PostgreSQL.

Os dados seguem por OTLP para a stack LGTM local. Métricas ficam no Prometheus, spans no Tempo e eventos de top queries no Loki. O Grafana provisiona o dashboard **Ordermod / PostgreSQL Overview** na pasta **Ordermod**.

## Subir o ambiente

```bash
docker compose up -d
./mvnw spring-boot:run
```

Abra http://localhost:3000/ e use `admin`/`admin` no primeiro acesso. Para acompanhar a inicialização:

```bash
docker compose ps
docker compose logs postgres-observability-init otel-collector-infra
```

O serviço transitório `postgres-observability-init` cria de forma idempotente a extensão `pg_stat_statements` nos bancos administrativo e da aplicação, além do usuário de coleta `postgres_monitor`. A senha local padrão é `monitor1234`; para substituí-la, defina `POSTGRES_MONITOR_PASSWORD` antes de iniciar o Compose.

## O que foi habilitado

O PostgreSQL inicia com `pg_stat_statements` em `shared_preload_libraries`, geração de query ID e medição de tempo de I/O e WAL. O coletor consulta o banco a cada 10 segundos e publica um evento de top queries a cada 30 segundos. A coleta automática de `EXPLAIN` e de amostras SQL está desabilitada.

A instrumentação JDBC mede as operações executadas e cria spans. Valores vinculados aos parâmetros não são capturados. Mesmo assim, nomes de tabelas, colunas e a estrutura normalizada dos comandos podem aparecer na telemetria; não coloque segredos em identificadores ou comentários SQL.

O coletor de infraestrutura acessa o socket Docker somente no ambiente local. Ele roda separado do coletor que recebe dados da aplicação, não expõe portas e filtra os contêineres para manter apenas a imagem do PostgreSQL.

## Consultas úteis

Validar a extensão e o usuário de monitoramento:

```bash
docker compose exec postgres psql -U myuser -d ordermod -c "SELECT extname FROM pg_extension WHERE extname = 'pg_stat_statements';"
docker compose exec postgres psql -U myuser -d ordermod -c "SELECT pg_has_role('postgres_monitor', 'pg_monitor', 'member');"
```

Se o dashboard estiver vazio, confirme nesta ordem: saúde do PostgreSQL e do LGTM, término bem-sucedido do init, logs do coletor e tráfego gerado pela aplicação. Algumas taxas e percentis precisam de pelo menos dois ciclos de coleta para aparecer.

## Limites desta configuração

Esta configuração é voltada ao desenvolvimento local: não contém alertas, retenção durável dimensionada, TLS ou gestão externa de segredos. Para produção, use credenciais em um secret manager, TLS entre componentes, acesso restrito ao Docker, retenção planejada e alertas definidos a partir de SLOs.
