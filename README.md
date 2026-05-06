# fullstack-crud-springboot-angular

Full stack CRUD application built with Spring Boot 4 (REST API) and Angular 17 (frontend) for managing clients and products.

## Tech Stack

**Backend**
- Java 17
- Spring Boot 4
- Spring Data JPA
- Spring Validation
- MySQL
- Lombok

**Frontend**
- Angular 17
- Bootstrap 5
- TypeScript

## Features

- Full CRUD for clients (name, email, phone, active status)
- Full CRUD for products (name, description, price, quantity, active status)
- DTO pattern separating API contract from database entities
- Bean Validation with descriptive error messages
- Global exception handler
- Responsive UI with Bootstrap

## Project Structure

```
├── backend/
│   └── clientes-api/          # Spring Boot REST API
│       └── src/main/java/com/clientes_api/
│           ├── controller/    # REST controllers + exception handler
│           ├── dto/           # Request and response DTOs
│           ├── model/         # JPA entities
│           ├── repository/    # Spring Data repositories
│           └── service/       # Business logic
│
└── frontend/
    └── clientes-front-v2/     # Angular app
        └── src/app/
            ├── pages/
            │   ├── clientes/  # Clients page (model, service, component)
            │   └── produtos/  # Products page (model, service, component)
            └── app.routes.ts
```

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL

### Backend

1. Configure your database in `backend/clientes-api/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

2. Run the API:

```bash
cd backend/clientes-api
./mvnw spring-boot:run
```

API will be available at `http://localhost:8080`

### Frontend

```bash
cd frontend/clientes-front-v2
npm install
ng serve
```

App will be available at `http://localhost:4200`

## API Endpoints

### Clients — `/api/clientes`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/clientes` | List all clients |
| GET | `/api/clientes/{id}` | Get client by id |
| POST | `/api/clientes` | Create client |
| PUT | `/api/clientes/{id}` | Update client |
| DELETE | `/api/clientes/{id}` | Delete client |

### Products — `/api/produtos`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/produtos` | List all products |
| GET | `/api/produtos/{id}` | Get product by id |
| POST | `/api/produtos` | Create product |
| PUT | `/api/produtos/{id}` | Update product |
| DELETE | `/api/produtos/{id}` | Delete product |
