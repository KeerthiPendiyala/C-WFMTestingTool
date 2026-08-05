FROM node:24.14.0-alpine AS frontend-build
WORKDIR /workspace
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY frontend/package.json frontend/package.json
RUN corepack enable && pnpm install --frozen-lockfile
COPY frontend/ frontend/
RUN pnpm -C frontend build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace
COPY backend/ backend/
COPY --from=frontend-build /workspace/frontend/dist frontend/dist
RUN cd backend && mvn -Dskip.frontend.resources=false -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /workspace/backend/app/target/app-0.1.0-SNAPSHOT.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
