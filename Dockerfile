FROM node:22-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM gradle:8.10.2-jdk17 AS backend
WORKDIR /workspace
COPY backend/ backend/
COPY assets/ assets/
COPY --from=frontend /workspace/frontend/dist/ backend/src/main/resources/static/
RUN gradle -p backend clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /workspace/backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
