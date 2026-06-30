# Therapeutica App

A web application for therapeutic support and management. This project contains a Java backend and a client-side UI (HTML + JavaScript). It provides core features for managing users, sessions, and therapy content in a secure, user-friendly interface.


## Features
- User authentication and role management (patients, therapists, admins)
- Create and manage therapy sessions and notes
- Static/user-facing UI built with HTML and light JavaScript
- REST API implemented in Java for backend operations
- Configurable persistence (database), environment-based settings

## Tech Stack
- Backend: Java (Spring Boot or similar)
- Frontend: HTML, CSS, JavaScript
- Build tools: (Maven or Gradle) — replace the examples below with the one your repo uses
- Optional: Docker for containerization

Languages used in the repo: Java, HTML, JavaScript.

## Architecture
- Java-based backend exposes REST endpoints under /api/*
- Frontend is served as static files from the server or as a simple static site
- Persistence layer supports an RDBMS (Postgres/MySQL) via JDBC or JPA
- Configurable via environment variables or application.properties / application.yml

## Prerequisites
- JDK 17+ (or the version required by your project)
- Maven or Gradle (depending on project)
- Node.js & npm (only if you use a frontend build step)

## Installation
1. Clone the repository:
   git clone https://github.com/elena33e/therapeutica-app.git
2. Change directory:
   cd therapeutica-app

## Running Locally

Backend (Maven example)
- Start using Maven:
  mvn clean spring-boot:run
- Or build and run jar:
  mvn clean package
  java -jar target/therapeutica-app-0.0.1-SNAPSHOT.jar

Backend (Gradle example)
- Run:
  ./gradlew bootRun
- Or build and run jar:
  ./gradlew build
  java -jar build/libs/your-app-name.jar

Frontend (static)
- If frontend files are static under src/main/resources/static they are served automatically by the backend.
- If you have a separate frontend build step (npm), run:
  npm install
  npm run build
  Then copy build output to the server static directory or serve separately.

## Building
- Maven:
  mvn clean package
- Gradle:
  ./gradlew build

## Testing
- Run backend unit tests:
  mvn test
  or
  ./gradlew test

- Add any frontend tests commands here if applicable.

## Configuration
Configuration is provided via application properties (application.properties or application.yml) or environment variables.

Common environment variables:
- SPRING_PROFILES_ACTIVE=dev|prod
- SERVER_PORT=8080
- DATABASE_URL=jdbc:postgresql://localhost:5432/therapeutica
- DATABASE_USERNAME=your_db_user
- DATABASE_PASSWORD=your_db_password
- JWT_SECRET=your_jwt_secret (if using JWT auth)

Replace the names above with the actual keys used in your project.


## Environment & Secrets
Never commit secrets (DB passwords, JWT secrets) to the repository. Use environment variables or a secrets manager in production.

## Contributing
- Fork the repo and create a feature branch: git checkout -b feat/your-feature
- Make changes with tests
- Open a pull request with a clear description and linked issue (if any)
- Follow the project's code style and commit message guidelines

## Contact
Maintainer: elena33e
Repository: https://github.com/elena33e/therapeutica-app
