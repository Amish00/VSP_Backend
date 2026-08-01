# ViriShare Backend

REST API for ViriShare — A Localized Video Sharing Platform for Nepal, built as a Final Year Project (CPP501) at Virinchi College / Asia e University.

## About

ViriShare is a full-stack video sharing platform designed specifically for the Nepali market. The backend provides a secure, role-based REST API supporting three portals — User, Creator, and Admin. It handles everything from authentication and video management to subscription billing, creator monetization, and admin moderation.

The monetization model distributes 70% of the monthly subscription revenue pool to creators based on weighted views and watch time, with 30% retained as platform revenue.

## Features

**Authentication & Security**
- JWT-based authentication with access and refresh tokens
- OAuth2 social login — Google, GitHub, and Outlook
- OTP-based two-factor authentication for password reset
- Role-based access control — USER, CREATOR, ADMIN
- BCrypt password hashing

**Video Management**
- Video upload via multipart form with Cloudinary CDN storage
- Video status workflow — Pending → Approved / Rejected
- Support for regular videos and Shorts (short-form content)
- Free and paid video types
- View count tracking and like/unlike

**Creator Features**
- Analytics — views, watch time, subscriber count, CTR
- Monthly earnings calculation based on weighted engagement scoring
- Payout request and processing (eSewa / Khalti)
- Subscriber management

**Payments**
- eSewa, Khalti, and Stripe payment gateway integration
- Subscription plans — Monthly VIEW, 6-Month VIEW, Yearly VIEW, Monthly CREATE
- Yearly plan revenue distributed monthly for stable pool calculation

**Admin**
- Video moderation with approve/reject and rejection reason
- User management — update role, plan, and account status
- Revenue reports and payout processing
- Platform-wide analytics

**Other**
- Comment system with pagination
- In-app notification delivery
- Multi-language support (i18n-ready)

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Security** — JWT + OAuth2
- **MySQL 8** + Spring Data JPA
- **Cloudinary** — video storage and CDN
- **eSewa / Khalti / Stripe** — payment gateways
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
