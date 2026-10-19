# Local Setup  

This guide helps you set up and run the API locally using Docker.

## Prerequisites

*   Docker needs to be installed and running.

## Local Database Setup
Prerequisite: DBeaver Community or another Database-Client that can host postgres needs to be installed.

Setup: a new Connection in the Database with
- `DB-Name = unogame`
- `Username = postgres`
- `Password = postgres`
- `Port = 5432`

To migrate the Database Schemas run `docker-compose -f localSetup/docker-compose.yaml up postgres -d` or start the SpringBoot-Service
## Steps

**Build and Run the API**
 
Navigate to the project root directory in your terminal and execute the following 
command:`docker compose -f localSetup/docker-compose.yaml up --build
`

If the execution was successfully the API can be reached under
`http://localhost:8081/swagger-ui/index.html`

**Run the API**

Once the API has been built, execute the following command to run it: `docker compose -f localSetup/docker-compose.yaml up`

**Test execution**

To run all Integration tests use the command:

macOS / Linux: `./main/gradlew -p main :backend:integrationTest`

Windows (PowerShell): `.\main\gradlew.bat -p main :backend:integrationTest`

To run all Smoke test use the command:

macOS / Linux: `./main/gradlew -p main :backend:smokeTest`

Windows (PowerShell): `.\main\gradlew.bat -p main :backend:smokeTest`

# OpenApi-Generator

To run the OpenApi-Generator manually the following
command must be executed in the main package: `./gradlew :backend:openApiGenerate`

# Debug-Setup

To debug an Integration-Test the following steps must be executed.

1. Start the docker-container
2. Create a new Remote JVM Debug Config with Host = localhost and port = 5005
3. Run the new Config in Debug mode
4. set a breakpoint somewhere in the code
5. Run the test in debug mode