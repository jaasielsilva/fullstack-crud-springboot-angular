# LexCRM - Enterprise Management System

A full-stack, enterprise-grade CRM and Inventory management application built with **Spring Boot 4** (REST API) and **Angular 17** (Frontend). 

This project evolved from a simple CRUD into a "Premium Dashboard" experience, focusing heavily on modern UI/UX principles, scalability, and performance.

## 🚀 Tech Stack

**Frontend (Modernized UI)**
- **Angular 17** (Standalone Components, Control Flow)
- **Bootstrap 5** (Customized with CSS utility classes)
- **Bootstrap Icons**
- **Google Fonts** (Inter Typography)
- **TypeScript**

**Backend (Robust API)**
- **Java 17**
- **Spring Boot 4**
- **Spring Data JPA & Hibernate**
- **Spring Validation**
- **MySQL**
- **Lombok**

## ✨ Premium Features & UX/UI

### 1. Modern Architecture & Layout
- **Global Sidebar & Topbar:** Fixed navigation layout mimicking top-tier enterprise SaaS platforms.
- **Responsive Grid:** Intelligent Flexbox and CSS Grid usage for seamless mobile-to-desktop transitions.

### 2. Advanced User Experience (UX)
- **Offcanvas Forms:** Forms slide in from the right edge, keeping the user in the context of the data table (100% width).
- **Skeleton Loading:** Replaced traditional spinners with shimmering skeleton placeholders for a perceived performance boost.
- **Custom Toast Notifications:** Elegant, non-intrusive floating notifications with animations for success/error feedback.
- **Smart Input Masking:** Real-time phone number formatting `(XX) XXXXX-XXXX` using custom Angular logic.

### 3. Backend Reliability
- Full CRUD for Clients and Products.
- **DTO Pattern:** Strict separation between API contracts and Database entities.
- **Global Exception Handling:** Standardized error JSON responses intercepted globally.
- **Bean Validation:** Clear and localized error messages.

## 📂 Project Structure

```
├── backend/
│   └── clientes-api/          # Spring Boot REST API
│       └── src/main/java/com/clientes_api/
│           ├── controller/    # REST controllers + Exception Handler
│           ├── dto/           # Request and response DTOs
│           ├── model/         # JPA Entities
│           ├── repository/    # Spring Data Repositories
│           └── service/       # Business Logic
│
└── frontend/
    └── clientes-front-v2/     # Angular 17 App
        └── src/
            ├── app/
            │   ├── pages/
            │   │   ├── dashboard/ # Analytics & KPIs
            │   │   ├── clientes/  # Clients Module
            │   │   └── produtos/  # Products Module
            │   ├── app.component.html # Global Layout (Sidebar/Topbar)
            │   └── app.routes.ts      # Routing Configuration
            ├── styles.css             # Global UI System (Skeletons, Toasts)
            └── index.html             # CDN Links (Fonts & Icons)
```

## 🛠️ Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL

### Backend Setup
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
*API will be available at `http://localhost:8080`*

### Frontend Setup
```bash
cd frontend/clientes-front-v2
npm install
ng serve
```
*App will be available at `http://localhost:4200`*

## 🔌 API Endpoints

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
