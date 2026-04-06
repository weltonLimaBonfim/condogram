# ══════════════════════════════════════════════════════════════════════════════
# Stage 1: BUILD
# Compila o projeto com Maven usando cache de dependências
# ══════════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copia só o pom.xml primeiro — aproveita cache do Docker se as deps não mudaram
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q 2>/dev/null || true

# Copia o código-fonte e compila
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -q && \
    # Extrai o JAR em camadas para o stage de runtime ser mais eficiente
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted


# ══════════════════════════════════════════════════════════════════════════════
# Stage 2: RUNTIME
# Imagem mínima — só JRE, sem Maven nem código-fonte
# ══════════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine AS runtime

# Metadados
LABEL maintainer="dev@condowhats.com.br"
LABEL org.opencontainers.image.title="CondoWhats API"
LABEL org.opencontainers.image.description="SaaS de gestão de condomínios via WhatsApp"

# Cria usuário não-root para rodar a aplicação (segurança)
RUN addgroup -S condowhats && adduser -S condowhats -G condowhats

# Diretório de logs
RUN mkdir -p /var/log/condowhats && chown -R condowhats:condowhats /var/log/condowhats

WORKDIR /app

# Copia as camadas extraídas na ordem certa (deps primeiro = melhor cache)
COPY --from=builder --chown=condowhats:condowhats /build/target/extracted/dependencies/    ./
COPY --from=builder --chown=condowhats:condowhats /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=condowhats:condowhats /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=condowhats:condowhats /build/target/extracted/application/    ./

USER condowhats

EXPOSE 8080

# Health check para Docker/K8s saber quando o container está pronto
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tunada para containers:
# -XX:+UseContainerSupport        → respeita os limites de CPU/memória do container
# -XX:MaxRAMPercentage=75.0       → usa 75% da RAM alocada para o container
# -XX:+UseG1GC                    → coletor de lixo balanceado
# -Djava.security.egd             → entropia rápida (evita travamento na inicialização)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", \
    "org.springframework.boot.loader.launch.JarLauncher"]
