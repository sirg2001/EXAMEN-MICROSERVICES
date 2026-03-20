# 🏢 EXAMEN-MICROSERVICES - Architecture Immobilière Complète

> **Architecture de microservices pour la gestion immobilière** - Examen Java & DevOps (M2 GL)  
> **Deadline:** 21 Mars 2026 | **Status:** ✅ Production-Ready

---

## 📌 Vue d'ensemble du projet

**EXAMEN-MICROSERVICES** est une architecture de microservices complète conçue pour gérer une plateforme de gestion immobilière. Elle démontre les concepts avancés de:

- ✅ Microservices distribués
- ✅ Communication **synchrone** (REST/HTTP)
- ✅ Communication **asynchrone** (Kafka/Events)
- ✅ Caching distribué (Redis)
- ✅ Persistance multi-base (MySQL isolée par service)
- ✅ Containerization (Docker)
- ✅ Orchestration (docker-compose)
- ✅ CI/CD (GitLab CI)
- ✅ Logging centralisé
- ✅ Exception handling standardisé

---

## 🎯 Domaine métier: Gestion Immobilière (Real Estate Management)

### Cas d'usage principal

1. **Propriétaire** ajoute une **propriété** avec détails (prix, localisation, capacité)
2. **Client** crée une **réservation** en sélectionnant une propriété et des dates
3. **Système de réservation** vérifie synchroniquement la disponibilité avec le catalogue
4. **Système de paiement** traite l'paiement asynchroniquement via Kafka lorsque la réservation est confirmée

### Entités métier

```
┌─────────────┐
│  Property   │  (Immobilier catalogue)
├─────────────┤
│ • id        │
│ • title     │
│ • location  │
│ • available │
│ • price     │
└─────────────┘
      ↑
      │ (sync REST call)
      │
┌─────────────┐          ┌──────────────┐
│  Booking    │─────────→│  Payment     │
├─────────────┤ Kafka    ├──────────────┤
│ • id        │ Event    │ • id         │
│ • propertyId           │ • bookingId  │
│ • dates     │          │ • amount     │
│ • status    │          │ • status     │
└─────────────┘          └──────────────┘
```

---

## 🏗️ Architecture système

### Vue d'ensemble des microservices

```
                         ┌─────────────────────────────┐
                         │   Client Application        │
                         │   (Web / Mobile / API)      │
                         └──────────┬────────────────┬─┘
                                    │                │
                                    ▼                ▼
                    ┌─────────────────────┐  ┌─────────────┐
                    │  MS-PROPERTIES      │  │ MS-BOOKINGS │◄──────┐
                    │  (Port 8081)        │  │ (Port 8082) │       │
                    │                     │  │             │       │
                    │ • CRUD Properties   │  │ • Manage    │       │
                    │ • Check Available   │  │   Bookings  │       │
                    │ • Cache: 10min      │──│ • Sync call │       │
                    └─────────────────────┘  │   to MSP    │       │
                           ▲                 │ • Publish   │       │
                           │                 │   to Kafka  │       │
                           │Cache (Redis)    └─────┬───────┘       │
                           │                       │               │
                           └──────────────────────────┘               │
                                                         Kafka Topic  │
                                              reservations-topic     │
                                                         │           │
                                                         ▼           │
                                            ┌──────────────────┐    │
                                            │  MS-PAYMENTS     │    │
                                            │  (Port 8083)     │    │
                                            │                  │    │
                                            │ • Consume events │    │
                                            │ • Create Payment │    │
                                            │ • Cache: 10min   │    │
                                            └──────────────────┘    │
                                                        │           │
                                                        └───────────┘
                                                   (async message)
```

### Infrastructure support

```
┌────────────────────────────────────────────────────┐
│                  Docker Network                    │
├────────────────────────────────────────────────────┤
│                                                    │
│  Zookeeper:2181  ─────┐                          │
│                        │                          │
│                      Kafka:9092                   │
│                        │                          │
│  ┌──────────────────────┼──────────────────┐     │
│  │                      │                  │     │
│  ▼                      ▼                  ▼     │
│ Redis:6379    MySQL:3306,3307,3308             │
│ (shared)      (isolated per service)            │
│                                                 │
└────────────────────────────────────────────────────┘
```

---

## 📦 Structure des répertoires

```
EXAMEN-MICROSERVICES/
│
├── MS-PROPERTIES/                          # Microservice 1: Property Catalog
│   ├── src/
│   │   └── main/java/com/realestate/properties/
│   │       ├── PropertiesApplication.java
│   │       ├── controller/
│   │       │   └── PropertyController.java        [7 endpoints]
│   │       ├── service/
│   │       │   └── PropertyService.java           [7 methods + cache]
│   │       ├── repository/
│   │       │   └── PropertyRepository.java        [Custom queries]
│   │       ├── entity/
│   │       │   └── Property.java                  [15 fields]
│   │       ├── dto/
│   │       │   └── PropertyDTO.java
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── PropertyNotFoundException.java
│   │       └── config/
│   │           └── CacheConfig.java
│   ├── Dockerfile                           [Multi-stage build]
│   ├── .gitlab-ci.yml                       [3-stage pipeline]
│   ├── pom.xml
│   └── README.md                            [Full API documentation]
│
├── MS-BOOKINGS/                            # Microservice 2: Reservation Management
│   ├── src/
│   │   └── main/java/com/realestate/bookings/
│   │       ├── BookingsApplication.java
│   │       ├── controller/
│   │       │   └── BookingController.java         [5 endpoints]
│   │       ├── service/
│   │       │   └── BookingService.java            [6 methods + sync call]
│   │       ├── repository/
│   │       │   └── BookingRepository.java
│   │       ├── entity/
│   │       │   └── Booking.java                   [9 fields + Status enum]
│   │       ├── dto/
│   │       │   └── BookingDTO.java
│   │       ├── client/
│   │       │   └── PropertyServiceClient.java     [RestTemplate client to MSP]
│   │       ├── event/
│   │       │   ├── ReservationCreatedEvent.java   [Kafka event DTO]
│   │       │   └── ReservationEventProducer.java  [Kafka publisher]
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── Custom exceptions
│   │       └── config/
│   │           └── KafkaConfig.java
│   ├── Dockerfile
│   ├── .gitlab-ci.yml
│   ├── pom.xml
│   └── README.md
│
├── MS-PAYMENTS/                            # Microservice 3: Payment Processing
│   ├── src/
│   │   └── main/java/com/realestate/payments/
│   │       ├── PaymentsApplication.java
│   │       ├── controller/
│   │       │   └── PaymentController.java         [3 endpoints GET]
│   │       ├── service/
│   │       │   └── PaymentService.java            [4 methods + Kafka processor]
│   │       ├── repository/
│   │       │   └── PaymentRepository.java
│   │       ├── entity/
│   │       │   └── Payment.java                   [7 fields + Status enum]
│   │       ├── dto/
│   │       │   └── PaymentDTO.java
│   │       ├── event/
│   │       │   ├── ReservationCreatedEvent.java   [Same event as MSB]
│   │       │   └── ReservationEventConsumer.java  [Kafka listener]
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── Custom exceptions
│   │       └── config/
│   │           └── KafkaConfig.java
│   ├── Dockerfile
│   ├── .gitlab-ci.yml
│   ├── pom.xml
│   └── README.md
│
├── docker-compose.yml                      [Complete orchestration]
├── README.md                                [This file]
├── ARCHITECTURE.md                          [Detailed architecture]
└── TESTING_SCENARIOS.md                     [End-to-end test cases]
```

---

## 🚀 Démarrage rapide

### Prérequis

- Docker & Docker Compose 20.10+
- Java 11+ (pour développement local)
- Maven 3.8+ (pour compilation locale)
- Git

### Option 1: Avec Docker Compose (Recommandé - Examen)

```bash
# Cloner/naviguer au répertoire
cd EXAMEN-MICROSERVICES

# Démarrer tous les services
docker-compose up -d

# Vérifier que tous les services sont actifs
docker-compose ps

# Regarder les logs
docker-compose logs -f ms-properties
docker-compose logs -f ms-bookings
docker-compose logs -f ms-payments
```

### Option 2: En développement local

#### Démarrer le infrastructure

```bash
# Terminal 1: Zookeeper & Kafka
docker-compose up -d zookeeper kafka redis mysql-properties mysql-bookings mysql-payments
```

#### Démarrer chaque microservice

```bash
# Terminal 2: MS-Properties
cd MS-PROPERTIES
mvn spring-boot:run

# Terminal 3: MS-Bookings
cd MS-BOOKINGS
mvn spring-boot:run

# Terminal 4: MS-Payments
cd MS-PAYMENTS
mvn spring-boot:run
```

### Vérification

```bash
# Test MS-Properties (port 8081)
curl http://localhost:8081/api/properties

# Test MS-Bookings (port 8082)
curl http://localhost:8082/api/bookings

# Test MS-Payments (port 8083)
curl http://localhost:8083/api/payments
```

---

## 🔄 Flux de communication inter-services

### Flux 1: Communication SYNCHRONE (REST/HTTP)

```
Client → MS-Bookings
    ↓ (createBooking request)
    
MS-Bookings.BookingService.createBooking()
    │
    ├─→ Appelle PropertyServiceClient.getAvailableProperty(propertyId)
    │   (HTTP GET request via RestTemplate)
    │
    └─→ MS-Properties.PropertyController.getAvailableProperty()
        │
        └─→ Retourne PropertyDTO si disponible
           OU PropertyNotAvailableException
```

**Code exemple (MS-Bookings):**
```java
@Autowired
private PropertyServiceClient propertyServiceClient;

public BookingDTO createBooking(BookingRequest request) {
    // 1. SYNC CALL: Vérifier la disponibilité
    PropertyDTO property = propertyServiceClient.getAvailableProperty(
        request.getPropertyId()
    );
    
    // 2. Si disponible, créer la réservation
    Booking booking = new Booking(...);
    bookingRepository.save(booking);
    
    // 3. ASYNC: Déclencher le paiement
    eventProducer.publishReservationCreatedEvent(booking);
    
    return mapToDTO(booking);
}
```

**Configuration (application.properties MS-Bookings):**
```properties
ms.properties.url=http://ms-properties:8081/api
```

**En cas d'erreur:** `PropertyNotAvailableException` → 409 Conflict

---

### Flux 2: Communication ASYNCHRONE (Kafka/Events)

```
MS-Bookings.ReservationEventProducer
    ↓ (publishReservationCreatedEvent)
    
    Kafka Topic: "reservations-topic"
    ├─ Message: ReservationCreatedEvent (JSON)
    │  {
    │    "bookingId": "BOOKING-001",
    │    "propertyId": "PROP-001",
    │    "customerEmail": "user@example.com",
    │    "totalPrice": 450.00,
    │    ...
    │  }
    │
    └─→ MS-Payments.ReservationEventConsumer
        (Kafka listener group: "payments-group")
        
        Automatiquement:
        PaymentService.processPaymentFromReservation(event)
        │
        └─→ Crée Payment entity
        └─→ Simule traitement
        └─→ Mark as COMPLETED
        └─→ Sauvegarde en DB
```

**Code exemple (MS-Bookings):**
```java
@Service
public class ReservationEventProducer {
    @Autowired
    private KafkaTemplate<String, ReservationCreatedEvent> kafkaTemplate;
    
    public void publishReservationCreatedEvent(Booking booking) {
        ReservationCreatedEvent event = new ReservationCreatedEvent(...);
        kafkaTemplate.send("reservations-topic", event.getBookingId(), event);
    }
}
```

**Code exemple (MS-Payments):**
```java
@Service
public class ReservationEventConsumer {
    @KafkaListener(topics = "reservations-topic", groupId = "payments-group")
    public void consumeReservationCreatedEvent(ReservationCreatedEvent event) {
        // Appelé automatiquement quand un événement arrive
        paymentService.processPaymentFromReservation(event);
    }
}
```

**Avantages asynchrone:**
- ✅ MS-Bookings ne dépend pas de la disponibilité de MS-Payments
- ✅ Paiements traités en arrière-plan
- ✅ Scalable: ajouter des consumers n'affecte pas le producteur
- ✅ Résilience: en cas d'erreur, retry automatique

---

## 📊 Contrats d'intégration

### Contract 1: MS-Bookings → MS-Properties (REST)

**Request:**
```
GET /api/properties/{id}/available

Response 200 OK:
{
  "id": 1,
  "title": "Apartment 2BR in Paris",
  "available": true,
  "pricePerNight": 100.00
}

Response 409 Conflict:
{
  "status": 409,
  "error": "Property Not Available",
  "message": "Property with id 1 is not available for these dates"
}
```

---

### Contract 2: MS-Bookings → Kafka (ReservationCreatedEvent)

**Topic:** `reservations-topic`  
**Format:** JSON (Spring Kafka JsonSerializer)

```json
{
  "bookingId": "BOOKING-001",
  "propertyId": "PROP-001",
  "customerName": "Jean Dupont",
  "customerEmail": "jean@example.com",
  "checkInDate": "2024-02-01",
  "checkOutDate": "2024-02-05",
  "nights": 4,
  "pricePerNight": 112.50,
  "totalPrice": 450.00,
  "createdAt": "2024-01-15T10:30:00"
}
```

---

## 🗄️ Schéma des bases de données

### MS-PROPERTIES Database

```sql
CREATE TABLE properties (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) UNIQUE NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  location VARCHAR(255),
  price_per_night DECIMAL(10, 2),
  total_bedrooms INT,
  total_bathrooms INT,
  available BOOLEAN DEFAULT TRUE,
  owner_name VARCHAR(100),
  owner_email VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### MS-BOOKINGS Database

```sql
CREATE TABLE bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id VARCHAR(50) UNIQUE NOT NULL,
  property_id VARCHAR(50) NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  customer_email VARCHAR(100) NOT NULL,
  check_in_date DATE NOT NULL,
  check_out_date DATE NOT NULL,
  status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### MS-PAYMENTS Database

```sql
CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id VARCHAR(50) UNIQUE NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
  transaction_id VARCHAR(50) UNIQUE NOT NULL,
  customer_email VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 🔌 Endpoints REST par service

### MS-PROPERTIES (Port 8081)

| Méthode | Endpoint | Responsabilité |
|---------|----------|-----------------|
| GET | `/api/properties` | Lister toutes les propriétés |
| GET | `/api/properties/{id}` | Détail d'une propriété |
| GET | `/api/properties/{id}/available` | Vérifier si propriété disponible (sync) |
| GET | `/api/properties/city/{city}` | Propriétés par ville |
| POST | `/api/properties` | Créer propriété |
| PUT | `/api/properties/{id}` | Mettre à jour propriété |
| DELETE | `/api/properties/{id}` | Supprimer propriété |

### MS-BOOKINGS (Port 8082)

| Méthode | Endpoint | Responsabilité |
|---------|----------|-----------------|
| GET | `/api/bookings` | Lister toutes les réservations |
| GET | `/api/bookings/{id}` | Détail d'une réservation |
| POST | `/api/bookings` | **CRÉER RÉSERVATION** (sync to MSP + async to Kafka) |
| PUT | `/api/bookings/{id}` | Mettre à jour réservation |
| DELETE | `/api/bookings/{id}` | Annuler réservation |

### MS-PAYMENTS (Port 8083)

| Méthode | Endpoint | Responsabilité |
|---------|----------|-----------------|
| GET | `/api/payments` | Lister tous les paiements |
| GET | `/api/payments/{id}` | Détail d'un paiement |
| GET | `/api/payments/customer/{email}` | Paiements par client |

---

## 💾 Stratégie de Cache (Redis)

Tous les services utilisent **Redis** avec **TTL de 10 minutes** (600 secondes):

### MS-Properties

```
@Cacheable("properties") → getPropertyById()
@Cacheable("properties-by-city") → getPropertiesByCity()
@CacheEvict(value = "properties", allEntries = true) → createProperty(), updateProperty(), deleteProperty()
```

### MS-Bookings

```
@Cacheable("bookings") → getBookingById()
@Cacheable("bookings-by-email") → getBookingsByCustomerEmail()
@CacheEvict(value = "bookings", allEntries = true) → createBooking(), updateBooking(), cancelBooking()
```

### MS-Payments

```
@Cacheable("payments") → getPaymentById()
@Cacheable("payments-by-email") → getPaymentsByCustomerEmail()
@CacheEvict(value = "payments", allEntries = true) → processPaymentFromReservation()
```

---

## 📊 Logging et Monitoring

### Configuration des logs

Tous les services loggent sur:
- **Console** : INFO et supérieur
- **Fichier** : `logs/{service}.log` (rotation quotidienne, max 30 jours)

**Exemples de logs importants:**

```
[MS-Properties] 2024-01-15 10:30:00 - INFO  - PropertyController - GET /api/properties/1
[MS-Bookings]   2024-01-15 10:30:05 - DEBUG - BookingService - Creating booking for property 1
[MS-Bookings]   2024-01-15 10:30:06 - INFO  - PropertyServiceClient - Calling MS-Properties API...
[MS-Bookings]   2024-01-15 10:30:07 - INFO  - ReservationEventProducer - Publishing event: BOOKING-001
[MS-Payments]   2024-01-15 10:30:08 - DEBUG - ReservationEventConsumer - === RECEIVED EVENT FROM KAFKA ===
[MS-Payments]   2024-01-15 10:30:08 - INFO  - PaymentService - Processing payment for booking BOOKING-001
[MS-Payments]   2024-01-15 10:30:09 - INFO  - PaymentService - Payment created: TXN-xxx
```

### Actuator Health Check

```bash
curl http://localhost:8081/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "database": "MySQL" },
    "redis": { "status": "UP" },
    "kafkaProducer": { "status": "UP" }
  }
}
```

---

## 🧪 Scénarios de test (End-to-End)

### Scénario 1: Créer une propriété

```bash
curl -X POST http://localhost:8081/api/properties \
  -H "Content-Type: application/json" \
  -d '{
    "code": "PARIS-001",
    "title": "Apartment 2BR Near Eiffel Tower",
    "price_per_night": 120,
    "location": "Paris, France",
    "owner_email": "owner@example.com"
  }'

# Response
{
  "id": 1,
  "code": "PARIS-001",
  "title": "Apartment 2BR Near Eiffel Tower",
  "available": true,
  "price_per_night": 120.00
}
```

### Scénario 2: Créer une réservation (DÉCLENCHE LES COMMUNICATIONS)

```bash
curl -X POST http://localhost:8082/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "PARIS-001",
    "customerName": "Jean Dupont",
    "customerEmail": "jean@example.com",
    "checkInDate": "2024-02-01",
    "checkOutDate": "2024-02-05"
  }'
```

**Ce qui se passe automatiquement:**

1. **MS-Bookings reçoit la requête**
   ```
   BookingController.createBooking()
   ```

2. **SYNC CALL: Vérification de disponibilité**
   ```
   BookingService appelle PropertyServiceClient.getAvailableProperty()
   → HTTP GET http://ms-properties:8081/api/properties/PARIS-001/available
   ← Retourne disponibilité
   ```

3. **Réservation créée et sauvegardée**
   ```
   Booking saved in ms_bookings database
   ```

4. **ASYNC EVENT: Publication sur Kafka**
   ```
   ReservationEventProducer.publishReservationCreatedEvent()
   → Message envoyé au topic "reservations-topic"
   ```

5. **MS-Payments consomme l'événement**
   ```
   ReservationEventConsumer reçoit l'événement
   → PaymentService.processPaymentFromReservation()
   → Payment créé et sauvegardé dans ms_payments database
   ```

### Réponse finale

```json
{
  "id": 1,
  "bookingId": "BOOKING-001-1705-1030",
  "propertyId": "PARIS-001",
  "customerName": "Jean Dupont",
  "customerEmail": "jean@example.com",
  "status": "CONFIRMED",
  "checkInDate": "2024-02-01",
  "checkOutDate": "2024-02-05",
  "createdAt": "2024-01-15T10:30:00"
}
```

### Scénario 3: Vérifier la création du paiement

```bash
# Attendre 2-3 secondes pour que Kafka traite l'événement

curl http://localhost:8083/api/payments/customer/jean@example.com

# Response
[
  {
    "id": 1,
    "bookingId": "BOOKING-001-1705-1030",
    "amount": 480.00,
    "status": "COMPLETED",
    "transactionId": "TXN-1705-1030-001",
    "customerEmail": "jean@example.com",
    "createdAt": "2024-01-15T10:30:01"
  }
]
```

---

## 📈 Dépendances Maven principales

Toutes les microservices partagent les mêmes dépendances:

```xml
<!-- Spring Boot -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA + Hibernate -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Driver -->
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
</dependency>

<!-- Spring Data Redis + Jedis -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Kafka -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Validation -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>
```

---

## 🐳 Orchestration Docker

### Démarrer tout

```bash
docker-compose up -d
```

**Services démarrés:**
- ✅ Zookeeper (2181)
- ✅ Kafka (9092)
- ✅ Redis (6379)
- ✅ MySQL Properties (3306)
- ✅ MySQL Bookings (3307)
- ✅ MySQL Payments (3308)
- ✅ MS-Properties (8081)
- ✅ MS-Bookings (8082)
- ✅ MS-Payments (8083)

### Arrêter tout

```bash
docker-compose down -v
```

### Logs en temps réel

```bash
docker-compose logs -f ms-bookings
```

---

## ✅ Checklist de validation (Avant examen)

Avant de soumettre le projet, vérifier:

- [ ] **Compilation**: `mvn clean install` réussit pour les 3 services
- [ ] **Docker Build**: `docker-compose build` sans erreurs
- [ ] **Docker Up**: `docker-compose up -d` démarre sans erreurs
- [ ] **Services Health**: Tous les endpoints `/actuator/health` retournent `UP`
- [ ] **Sync Communication**: `MS-Bookings` appelle bien `MS-Properties`
- [ ] **Async Communication**: Kafka consomme bien les événements
- [ ] **Cache**: Redis cache les réponses correctement
- [ ] **Logging**: Logs détaillés dans `logs/` pour chaque service
- [ ] **README**: Chaque service a une documentation complète

### Commande de validation complète

```bash
# 1. Compiler
mvn clean install -DskipTests

# 2. Démarrer infrastructure
docker-compose up -d

# 3. Attendre 30 secondes pour initialisation

# 4. Test MS-Properties
curl http://localhost:8081/api/properties

# 5. Test MS-Bookings
curl http://localhost:8082/api/bookings

# 6. Test MS-Payments
curl http://localhost:8083/api/payments

# 7. Health checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

---

## 📚 Documents supplémentaires

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Diagrammes détaillés et flux
- [TESTING_SCENARIOS.md](./TESTING_SCENARIOS.md) - Cas de test complets
- [MS-PROPERTIES/README.md](./MS-PROPERTIES/README.md) - Documentation détaillée
- [MS-BOOKINGS/README.md](./MS-BOOKINGS/README.md) - Documentation détaillée
- [MS-PAYMENTS/README.md](./MS-PAYMENTS/README.md) - Documentation détaillée

---

## 🎓 Concepts démontrés

### Architecture & Design
- ✅ Microservices architecture
- ✅ Domain-Driven Design (DDD)
- ✅ Separation of Concerns (Controller→Service→Repository)
- ✅ API-First Design (REST contracts)

### Communication
- ✅ Synchronous HTTP/REST (RestTemplate)
- ✅ Asynchronous Messaging (Apache Kafka)
- ✅ Event-Driven Architecture
- ✅ Service-to-Service Communication Patterns

### Persistance
- ✅ JPA/Hibernate ORM
- ✅ Entity Relationships
- ✅ Database per Service Pattern
- ✅ Repository Pattern

### Caching
- ✅ Distributed Caching (Redis)
- ✅ Cache-Aside Pattern
- ✅ Cache Invalidation

### DevOps & Infrastructure
- ✅ Containerization (Docker, multi-stage builds)
- ✅ Container Orchestration (docker-compose)
- ✅ Config Management (environment variables)
- ✅ CI/CD Pipelines (GitLab CI)
- ✅ Health Checks & Monitoring

### Code Quality
- ✅ Logging (SLF4J, Logback)
- ✅ Exception Handling
- ✅ Validation (JSR-380)
- ✅ Error Responses Standardization

---

## 🚨 Troubleshooting

### Service refuse la connexion

```bash
# Vérifier que Docker est en cours d'exécution
docker ps

# Vérifier que le service est actif
docker-compose logs ms-properties

# Redémarrer le service
docker-compose restart ms-properties
```

### Kafka Events ne sont pas traités

```bash
# Vérifier que Kafka est actif
docker-compose logs kafka

# Vérifier les logs MS-Payments
docker-compose logs ms-payments

# Topic existe-t-il?
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### MySQL refuses connection (Access denied)

```bash
# Vérifier les credentials dans docker-compose.yml
# Par défaut: root/root

# Réinitialiser les bases
docker-compose down -v
docker-compose up -d
```

---

## 📞 Support & Ressources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Docker Documentation**: https://docs.docker.com/
- **Maven Guide**: https://maven.apache.org/guides/

---

## 📄 License

Projet académique pour examen M2 GL (21 Mars 2026)  
Tous droits réservés à l'institution éducative.

---

**Dernière mise à jour:** 2024-01-15  
**Status:** ✅ **PRODUCTION READY**  
**Version:** 1.0.0
