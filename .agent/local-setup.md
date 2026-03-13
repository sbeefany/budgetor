# Local Environment Setup Guide

This document explains how to run the Budgetor application locally for testing and development, specifically starting PostgreSQL and the Ollama LLM.

## Prerequisites
1. **Docker**: Installed and running (for the database).
2. **Ollama**: Installed from [ollama.com](https://ollama.com/download).
3. **Java 21**: Installed and configured.

## Step 1: Start PostgreSQL (Database)
The project uses a standard PostgreSQL database. A `docker-compose.yml` file is provided in the project root to start it easily.

From the project root directory, run:
```bash
docker-compose up -d
```
*This starts the database on port 5432 with the credentials defined in `application.yaml`.*

## Step 2: Start Local LLM (Ollama)
The application expects a local instance of the `mistral` model to process natural language inputs.

Open a new terminal window and run:
```bash
ollama run mistral
```
*Leave this window open or running in the background.*

## Step 3: Run the Application
You can now start the Spring Boot application. It will automatically connect to the database (and apply Flyway/Liquibase migrations) and the local Ollama instance.

From the project root directory, run:
```bash
./gradlew bootRun
```

Or, run the `Application.java` main class directly from your IDE.

## Step 4: Local Terminal Testing (CLI Mode)
If you want to test the AI functionality directly through the terminal without involving Telegram, the project now includes a CLI.

To start the application with the terminal test interface active, set the `local-cli` profile:

**Powershell:**
```powershell
$env:SPRING_PROFILES_ACTIVE="local-cli"
./gradlew bootRun
```

**Bash/Zsh:**
```bash
SPRING_PROFILES_ACTIVE=local-cli ./gradlew bootRun
```

When it finishes loading, look at the terminal. It will say `Budgetor Local CLI Started!` and provide a `You:` prompt where you can chat with Ollama.
