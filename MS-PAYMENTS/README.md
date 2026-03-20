# MS-PAYMENTS - Microservice de Gestion des Paiements

## 📋 Vue d'ensemble

**MS-Payments** est un microservice responsable du traitement des paiements dans l'architecture de gestion immobilière. Il consomme les événements Kafka publiés par MS-Bookings et crée automatiquement des enregistrements de paiement.

### Caractéristiques principales

- ✅ **Consommateur Kafka** : Écoute les événements `ReservationCreatedEvent` sur le topic `reservations-topic`
- ✅ **Traitement asynchrone** : Crée des paiements sans bloquer le flux de réservation
- ✅ **Idempotence** : Empêche la création de doublons si le même événement arrive plusieurs fois
- ✅ **Cache Redis** : Améliore les performances des recherches de paiement
- ✅ **Logging détaillé** : Enregistre toutes les opérations pour le debugging
- ✅ **Gestion d'exceptions** : Réponses d'erreur standardisées et prévisibles
- ✅ **Dockerisé** : Build multi-stage avec OpenJDK 11 JRE

## 🏗️ Architecture

### Port et Configuration

- **Port application** : `8083`
- **Base de données** : MySQL sur port `3308`
- **Cache** : Redis (port 6379, TTL 10 minutes)
- **Message Broker** : Kafka topic `reservations-topic`

### Flux de communication

```
MS-Bookings (crée réservation)
    ↓
    ├─→ Publishes ReservationCreatedEvent to Kafka
    └─→ Returns booking confirmation
         ↓
    MS-Payments (écoute Kafka)
         ↓
         Crée Payment + Simule traitement
         ↓
         Sauvegarde Payment avec status COMPLETED
```

## 📦 Structure du projet

```
MS-PAYMENTS/
├── src/
│   ├── main/
│   │   ├── java/com/realestate/payments/
│   │   │   ├── PaymentsApplication.java          # Point d'entrée
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java        # Endpoints REST
│   │   │   ├── service/
│   │   │   │   └── PaymentService.java           # Logique métier
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java        # Accès données
│   │   │   ├── entity/
│   │   │   │   └── Payment.java                  # Entité JPA
│   │   │   ├── dto/
│   │   │   │   └── PaymentDTO.java               # Objet de transfert
│   │   │   ├── event/
│   │   │   │   ├── ReservationCreatedEvent.java  # Événement Kafka
│   │   │   │   └── ReservationEventConsumer.java # Consommateur Kafka
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java   # Gestion erreurs
│   │   │   │   ├── PaymentNotFoundException.java
│   │   │   │   ├── PaymentAlreadyExistsException.java
│   │   │   │   └── PaymentProcessingException.java
│   │   │   └── config/
│   │   │       └── KafkaConfig.java              # Configuration Kafka
│   │   └── resources/
│   │       └── application.properties            # Propriétés
│   └── test/
├── Dockerfile                                     # Container
├── .gitlab-ci.yml                                # Pipeline CI/CD
├── pom.xml                                        # Dépendances Maven
└── README.md                                      # Documentation
```

## 🔌 Endpoints REST

### GET /api/payments/{id}
Récupère un paiement par ID

**Response 200:**
```json
{
  "id": 1,
  "bookingId": "BOOKING-001",
  "amount": 450.00,
  "status": "COMPLETED",
  "transactionId": "TXN-1699500000000",
  "customerEmail": "user@example.com",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:01"
}
```

### GET /api/payments
Récupère tous les paiements

**Response 200:**
```json
[
  {
    "id": 1,
    "bookingId": "BOOKING-001",
    "amount": 450.00,
    "status": "COMPLETED",
    "transactionId": "TXN-1699500000000",
    "customerEmail": "user@example.com",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:01"
  }
]
```

### GET /api/payments/customer/{email}
Récupère tous les paiements d'un client

**Response 200:**
```json
[
  {
    "id": 1,
    "bookingId": "BOOKING-001",
    "amount": 450.00,
    "status": "COMPLETED",
    "transactionId": "TXN-1699500000000",
    "customerEmail": "user@example.com",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:01"
  }
]
```

## 🎯 Entités et DTOs

### Payment Entity

```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bookingId;      // Référence à la réservation
    private BigDecimal amount;     // Montant du paiement
    private PaymentStatus status;  // PENDING, COMPLETED, FAILED
    private String transactionId;  // Identifiant unique de transaction
    private String customerEmail;  // Email du client
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**PaymentStatus enum:**
- `PENDING` : Paiement en attente de traitement
- `COMPLETED` : Paiement réussi
- `FAILED` : Paiement échoué

## 📨 Événements Kafka

### ReservationCreatedEvent (Consommé)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedEvent implements Serializable {
    private String bookingId;
    private String propertyId;
    private String customerName;
    private String customerEmail;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int nights;
    private BigDecimal pricePerNight;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}
```

**Topic:** `reservations-topic`  
**Consumer Group:** `payments-group`  
**Format:** JSON (via Spring Kafka JsonDeserializer)

### Consommateur Kafka

Le `ReservationEventConsumer` écoute automatiquement le topic et traite les événements:

```java
@KafkaListener(topics = "reservations-topic", groupId = "payments-group")
public void consumeReservationCreatedEvent(ReservationCreatedEvent event) {
    // Crée automatiquement un paiement
    paymentService.processPaymentFromReservation(event);
}
```

## 🔄 Flux de traitement des paiements

1. **MS-Bookings** publie `ReservationCreatedEvent` sur Kafka
2. **MS-Payments** reçoit l'événement via `ReservationEventConsumer`
3. **PaymentService** traite l'événement:
   - Vérifie que le paiement n'existe pas déjà (idempotence)
   - Crée une entité `Payment`
   - Génère un identifiant de transaction unique
   - Simule le traitement (Thread.sleep(1000))
   - Met à jour le statut à `COMPLETED`
   - Sauvegarde dans la base de données

## 🗄️ Base de données

### Schéma MySQL (ms_payments)

```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    customer_email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_booking_id ON payments(booking_id);
CREATE INDEX idx_customer_email ON payments(customer_email);
CREATE INDEX idx_status ON payments(status);
```

## 💾 Cache Redis

Tous les paiements sont mis en cache avec une TTL de **10 minutes**:

```properties
spring.cache.redis.time-to-live=600000
```

**Annotations de cache:**
- `@Cacheable("payments")` : Récupère du cache si existant
- `@CacheEvict(value = "payments", allEntries = true)` : Invalide le cache lors des modifications

## 🚀 Démarrage du service

### Localement (development)

```bash
# Prérequis
# - MySQL 8 sur port 3308
# - Redis sur port 6379
# - Kafka sur port 9092

# Compiler et exécuter
mvn clean install
mvn spring-boot:run

# Service disponible sur http://localhost:8083/api/payments
```

### Avec Docker

```bash
# Voir docker-compose.yml du projet root pour orchestration complète
docker build -t ms-payments:latest .
docker run -p 8083:8083 \
  -e "SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3308/ms_payments" \
  -e "SPRING_REDIS_HOST=redis" \
  -e "SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092" \
  ms-payments:latest
```

## 🧪 Tests

### Tests de base

```bash
# Vérifier que le service est actif
curl http://localhost:8083/api/payments

# Créer une réservation dans MS-Bookings (publie event)
curl -X POST http://localhost:8082/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": "PROP-001",
    "customerName": "Jean Dupont",
    "customerEmail": "jean@example.com",
    "checkInDate": "2024-02-01",
    "checkOutDate": "2024-02-05"
  }'

# Vérifier que le paiement a été créé automatiquement
curl http://localhost:8083/api/payments/customer/jean@example.com
```

### Test Kafka (consommateur)

```bash
# Publier un événement de test directement
docker exec -it kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic reservations-topic \
  --property value.serializer=org.apache.kafka.common.serialization.StringSerializer << 'EOF'
{
  "bookingId": "TEST-001",
  "propertyId": "PROP-001",
  "customerName": "Test User",
  "customerEmail": "test@example.com",
  "checkInDate": "2024-02-01",
  "checkOutDate": "2024-02-05",
  "nights": 4,
  "pricePerNight": 100.00,
  "totalPrice": 400.00,
  "createdAt": "2024-01-15T10:00:00"
}
EOF

# Vérifier que le paiement a été créé
curl http://localhost:8083/api/payments
```

## 🔍 Logs

Les logs sont enregistrés dans `logs/ms-payments.log`:

```
2024-01-15 10:30:00 - INFO  - PaymentController - GET /api/payments/customer/user@example.com
2024-01-15 10:30:05 - DEBUG - ReservationEventConsumer - === RECEIVED EVENT FROM KAFKA ===
2024-01-15 10:30:05 - INFO  - PaymentService - Processing payment from Kafka event for booking: BOOKING-001
2024-01-15 10:30:06 - INFO  - PaymentService - Payment created successfully: PaymentDTO(id=1, ...)
```

## ⚠️ Gestion des erreurs

### Exceptions gérées

```
PaymentNotFoundException          → 404 Not Found
PaymentAlreadyExistsException     → 409 Conflict
PaymentProcessingException        → 500 Internal Server Error
Exception générale                → 500 Internal Server Error
```

**Réponse d'erreur standardisée:**
```json
{
  "timestamp": "2024-01-15T10:30:05",
  "status": 404,
  "error": "Payment Not Found",
  "message": "Payment with id 999 not found",
  "path": "/api/payments/999"
}
```

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8083/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "result": 1
      }
    },
    "redis": {
      "status": "UP"
    },
    "kafkaProducer": {
      "status": "UP"
    }
  }
}
```

### Métriques

```bash
curl http://localhost:8083/actuator/metrics
```

## 🔐 Sécurité

- ✅ Validation JSR-380 sur tous les DTOs
- ✅ Exception handling sans exposition de stacktrace en production
- ✅ Logging des tentatives d'accès non autorisé
- ✅ Isolation des bases de données par microservice
- ✅ Credentials externalisés (environment variables en production)

## 📝 Dépendances clés

- **Spring Boot 2.7.14** : Framework web
- **Spring Data JPA** : ORM avec Hibernate
- **Spring Kafka** : Consommation d'événements asynchrones
- **Spring Cache** : Intégration Redis
- **MySQL Connector** : Driver JDBC
- **Lombok** : Réduction du boilerplate
- **Jedis** : Client Redis
- **Jackson** : Sérialisation JSON

## 🐳 Docker & Kubernetes

### Dockerfile

Build multi-stage:
1. **Stage 1** : Maven compile l'application
2. **Stage 2** : OpenJDK 11 JRE exécute le JAR

```dockerfile
FROM maven:3.8.1-openjdk-11 AS builder
# Build application
FROM openjdk:11-jre-slim
# Run application
```

### GitLab CI/CD

Pipeline 3 stages:
1. **build:maven** : Compile le code (`mvn clean package`)
2. **package:docker** : Crée l'image Docker
3. **push:registry** : Pousse vers Docker Hub

## 🤝 Communication inter-services

### Dépendances vis-à-vis d'autres services

- ⬅️ **Consomme les événements de** : MS-Bookings (via Kafka)
- ➡️ **Notifie** : (aucune - c'est le dernier maillon)

### Contrats d'intégration

**Entrée (Kafka Event):** ReservationCreatedEvent
```json
{
  "bookingId": "BOOKING-001",
  "totalPrice": 450.00,
  "customerEmail": "user@example.com"
}
```

**Sortie (REST API):** PaymentDTO
```json
{
  "id": 1,
  "bookingId": "BOOKING-001",
  "amount": 450.00,
  "status": "COMPLETED",
  "transactionId": "TXN-1699500000000"
}
```

## 📚 Références

- [Documentation Spring Kafka](https://spring.io/projects/spring-kafka)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Cache Abstraction](https://spring.io/projects/spring-framework)
- [Kafka Topics & Consumer Groups](https://kafka.apache.org/documentation/#intro_topics)

## 👨‍💼 Support

Pour toute question sur MS-Payments:
- Consultez les logs dans `logs/ms-payments.log`
- Vérifiez la connectivité MySQL sur port 3308
- Vérifiez que Kafka est actif sur port 9092
- Testez l'endpoint `/api/payments` directement

---

**Dernière mise à jour:** 2024-01-15  
**Version:** 1.0.0  
**Status:** Production-Ready ✅
