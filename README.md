# ViriShare Backend

REST API for ViriShare — A Localized Video Sharing Platform for Nepal.

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Security** — JWT authentication + OAuth2 (Google, GitHub, Outlook)
- **MySQL 8** + Spring Data JPA
- **Cloudinary** — video storage and CDN delivery
- **eSewa / Khalti / Stripe** — payment gateway integration
- **BCrypt** — password hashing
- **Maven** — build tool

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+

## Getting Started

```bash
git clone https://github.com/Amish00/VSP_Backend.git
cd VSP_Backend
```

Create the database:

```sql
CREATE DATABASE virishare_db;
```

Configure `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/virishare_db
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

app.jwt.secret=YOUR_JWT_SECRET
app.jwt.expiration-ms=900000
app.jwt.refresh-expiration-ms=604800000

cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET

esewa.merchant-code=YOUR_ESEWA_MERCHANT_CODE
khalti.secret-key=YOUR_KHALTI_SECRET_KEY
stripe.secret-key=YOUR_STRIPE_SECRET_KEY

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
```

Run:

```bash
mvn spring-boot:run
```

API runs at `http://localhost:8080`.

## Related

- [ViriShare Frontend](https://github.com/Amish00/VSP_Frontend)
- CPP501 Final Year Project — Virinchi College / Asia e University
