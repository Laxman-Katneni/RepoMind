# AI Code Review Assistant - Java Migration

This is the Java Spring Boot 3.2 version of the AI-Powered Code Review Assistant, migrated from Python.

## Technology Stack

- **Spring Boot**: 3.2.0
- **Java**: 17
- **Database**: PostgreSQL
- **Cache/Queue**: Redis
- **AI/LLM**: Spring AI with OpenAI
- **Build Tool**: Maven

## Dependencies

- `spring-boot-starter-web` - REST API endpoints
- `spring-boot-starter-data-jpa` - Database access with JPA
- `postgresql` - PostgreSQL database driver
- `lombok` - Reduce boilerplate code
- `spring-ai-openai-spring-boot-starter` - AI/LLM integration
- `spring-boot-starter-data-redis` - Redis for async queues

## Package Structure

```
com.reviewassistant/
├── auth/          - Authentication and authorization
├── config/        - Application configuration
├── controller/    - REST API controllers
├── model/         - Domain models, entities, DTOs (replaces Python 'data' and 'metrics')
├── repository/    - Data access layer (JPA repositories)
├── service/       - Business logic (handles ingestion, indexing, pr, rag)
└── util/          - Utility classes and helpers
```

## Configuration

Update the following in `src/main/resources/application.properties`:

1. **Database**: Set PostgreSQL connection details
2. **Redis**: Configure Redis host and port
3. **OpenAI**: Add your OpenAI API key

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Next Steps

1. Configure database credentials in application.properties
2. Set up OpenAI API key
3. Migrate Python business logic to Java services
4. Implement REST controllers based on original Python endpoints
5. Create JPA entities for your data models
6. Set up Redis queue handlers for async processing
