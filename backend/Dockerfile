# se construye desde la raiz del repo: docker build -f backend/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew ./
COPY gradle/wrapper gradle/wrapper
COPY shared/src shared/src
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/
COPY backend/src/main backend/src/main
RUN ./gradlew -p backend installDist --no-daemon -q

FROM eclipse-temurin:21-jre
# "hoy" y los datos de prueba dependen de la zona horaria de las estancias
ENV TZ=America/Monterrey
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
RUN useradd --system --create-home sonrisas
WORKDIR /app
COPY --from=build /src/backend/build/install/backend /app
# sin DATABASE_URL usa h2 en /app/datos
RUN mkdir /app/datos && chown sonrisas /app/datos
USER sonrisas
EXPOSE 8080
CMD ["/app/bin/backend"]
