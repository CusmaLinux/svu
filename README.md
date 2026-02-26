<div align="center">
  <img src="src/main/webapp/content/images/logo-svu.svg" alt="SVU Logo" width="200"/>
  <h1>Sistema de Ventanilla Única (SVU)</h1>
  <p><strong>Management system for PQRS (Peticiones, Quejas, Reclamos y Sugerencias) at the Technological Institute of Putumayo.</strong></p>
</div>

---

## Introduction

The **Sistema de Ventanilla Única (SVU)** is a centralized platform designed to streamline and manage the lifecycle of citizen requests (PQRS) at the UniPutumayo. Built with a focus on transparency, efficiency, and traceability, it allows both citizens and administrative staff to interact seamlessly.

This project is built using the **JHipster** platform, leveraging **Spring Boot** for the backend, **Vue.js** for the frontend, and **MongoDB** as the primary data store.

---

## Setup & Development

### 1. Local Setup (Without Docker)

To run the application locally, ensure you have the following installed:

- **Java 17**
- **Node.js 20+**
- **MongoDB** (Running locally or accessible via URI)

#### Step-by-Step Instructions:

1. **Clone the repository:**

   ```bash
   git clone <repository-url>
   cd svu
   ```

2. **Configure Environment Variables:**
   The application requires several environment variables for security, mail, and external APIs.

   - Copy `src/main/resources/.env.example` to `src/main/resources/.env` (or set them in your environment).
   - Fill in the required values:
     - `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD`: For email notifications.
     - `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET`: Your JWT signing key.
     - `GOOGLE_RECAPTCHA_SECRET_KEY`: For bot protection.
     - `GEMINI_API_KEY`: For AI-powered features (OCR analysis, etc).

3. **Install Dependencies:**

   ```bash
   ./npmw install
   ```

4. **Run the Backend (Spring Boot):**
   In one terminal, run:

   ```bash
   ./mvnw
   ```

5. **Run the Frontend (Vue.js SPA):**
   In another terminal, run:
   ```bash
   ./npmw start
   ```
   The application will be available at `http://localhost:9000` (with hot-reload) or `http://localhost:8080`.

#### Optional Database with Docker:

If you don't have MongoDB installed locally, you can start it using Docker:

```bash
docker compose -f src/main/docker/mongodb.yml up -d
```

### 2. PWA Support

JHipster ships with PWA (Progressive Web App) support, which is turned off by default. To enable it, uncomment the service worker registration in `src/main/webapp/index.html`:

```html
<script>
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./service-worker.js').then(function () {
      console.log('Service Worker Registered');
    });
  }
</script>
```

---

## Building for Production

### Packaging as JAR

To build the final optimized JAR for production:

```bash
./mvnw -Pprod clean verify
```

This will concatenate and minify client CSS and JS files. To run the resulting JAR:

```bash
java -jar target/*.jar
```

### Packaging as WAR

To package as a WAR for deployment to an application server:

```bash
./mvnw -Pprod,war clean verify
```

---

## Setup with Docker

You can run the entire stack (Application + Database) using Docker Compose.

1. **Build the application image:**

   ```bash
   ./mvnw -Pprod verify jib:dockerBuild
   ```

2. **Launch the stack:**
   ```bash
   docker compose -f src/main/docker/app.yml up -d
   ```

---

## Environments

| Environment     | Purpose                        | URL / Access                                                      |
| --------------- | ------------------------------ | ----------------------------------------------------------------- |
| **Development** | Local development and testing. | `http://localhost:9000` (Frontend), `http://localhost:8080` (API) |
| **Production**  | Live system for end users.     | [https://svu.keliumju.com](https://svu.keliumju.com)              |

---

## Testing

Verification is a key part of our development lifecycle.

### API (Backend) Testing

We use **JUnit** for unit and integration testing.

```bash
./mvnw verify
```

### Webapp (SPA) Testing

We use **Vitest** for frontend unit testing.

```bash
npm test
```

---

## Code Quality & CI

We maintain high code quality standards using **SonarQube**. Every Pull Request and push to the `main` branch triggers an automated analysis in our CI pipeline (GitHub Actions).

### Local Sonar Analysis

To analyze code quality locally:

1. **Start the Sonar server:**
   ```bash
   docker compose -f src/main/docker/sonar.yml up -d
   ```
2. **Run the analysis:**
   ```bash
   ./mvnw initialize sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
   ```
   _Note: Access the UI at http://localhost:9001_

### Cloud Integration

- **SonarCloud Organization:** `cusmalinux`
- **Project Key:** `CusmaLinux_svu`

---

## Contributing

We welcome contributions! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) for details on our workflow, commit conventions (Trunk-Based Development), and how to set up your environment (SSH/GPG keys).

---

## License

This project is licensed under the terms specified in the [package.json](package.json) file.
