# ARCHITECTURE.md - Système Distribué Complet

Documentation détaillée de l'architecture système avec diagrammes et flux.

## 1. Architecture Général du Système

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT APPLICATIONS                     │
│              (Web / Mobile / REST Clients)                  │
└────────────────────────┬────────────┬────────────┬──────────┘
                         │            │            │
       ┌─────────────────┴──┐  ┌──────┴──────┐  ┌──┴────────┐
       │                    │  │             │  │           │
       ▼                    ▼  ▼             ▼  ▼           ▼
  ┌──────────┐      ┌──────────────┐      ┌──────────┐
  │ HTTP GET │      │ HTTP POST/   │      │ HTTP GET │
  │Properties│      │ PUT DELETE   │      │Payments  │
  │          │      │ Bookings     │      │          │
  └──────────┘      └──────────────┘      └──────────┘
       │                    │                    │
       │            ┌───────┼────────┐           │
       │            │       │        │           │
       ▼            ▼       │        ▼           ▼
  ┌─────────────────┐       │  ┌─────────────────────┐
  │ MS-PROPERTIES   │       │  │ MS-PAYMENTS         │
  │ (Port 8081)     │       │  │ (Port 8083)         │
  │                 │       │  │                     │
  │ • Property DB   │       │  │ • Payment DB        │
  │ • Get, Post,    │       │  │ • Kafka Consumer    │
  │   Put, Delete   │◄──────┤  │ • Cache (Redis)     │
  │ • Cache (Redis) │       │  │                     │
  └─────────────────┘       │  └─────────────────────┘
           ▲                │
           │                │
      (Sync Call)      ┌────▼────────────────┐
      (REST HTTP)      │ MS-BOOKINGS         │
                       │ (Port 8082)         │
                       │                     │
                       │ • Booking DB        │
                       │ • Sync to MSP       │
                       │ • Publish to Kafka  │
                       │ • Cache (Redis)     │
                       └─────┬──────────────┘
                             │
                       (Kafka Publish)
                       ┌─────▼──────────┐
                       │ Kafka Topic:   │
                       │ reservations   │
                       │ -topic         │
                       └─────┬──────────┘
                             │
                       (Kafka Consume)
                             │
                       (Processed by MS-Payments)
```

---

## 2. Flux de Création de Réservation (End-to-End)

```
STEP 1: Client crée une réservation
═════════════════════════════════════

Client
  │
  └──→ POST /api/bookings
       {
         "propertyId": "PROP-001",
         "customerEmail": "user@example.com",
         "checkInDate": "2024-02-01",
         "checkOutDate": "2024-02-05"
       }

STEP 2: MS-Bookings reçoit la requête
══════════════════════════════════════

BookingController.createBooking()
  │
  └──→ BookingService.createBooking()
       │
       ├─→ Valider le DTO (JSR-380)
       │
       └─→ SYNC CALL: Vérifier la disponibilité
           │
           └──→ PropertyServiceClient.getAvailableProperty()
               │
               ├─→ URL: http://ms-properties:8081/api/properties/PROP-001/available
               │
               ├─→ Si ERREUR: Lance PropertyNotAvailableException
               │              Retour 409 Conflict au client
               │
               └─→ Si OK: Reçoit PropertyDTO

STEP 3: Réservation validée
════════════════════════════

BookingService.createBooking()
  │
  └──→ Crée Booking entity
       ├─ bookingId = "BOOKING-001-160524-001"
       ├─ status = PENDING → CONFIRMED
       ├─ propertyId = PROP-001
       └─ customerEmail = user@example.com
       │
       └──→ bookingRepository.save(booking)
            [Sauvegarde dans MySQL ms_bookings:3307]

STEP 4: Publication Kafka Event (ASYNC)
════════════════════════════════════════

BookingService.createBooking()
  │
  └──→ ReservationEventProducer.publishReservationCreatedEvent()
       │
       └──→ kafkaTemplate.send("reservations-topic", event)
            │
            │ Message:
            │ {
            │   "bookingId": "BOOKING-001",
            │   "propertyId": "PROP-001",
            │   "totalPrice": 450.00,
            │   "customerEmail": "user@example.com"
            │ }
            │
            └──→ Kafka Topic "reservations-topic"
                 [Non-bloquant - retour immédiat au client]

STEP 5: Réponse au client
═════════════════════════

BookingController
  │
  └──→ return ResponseEntity.ok(bookingDTO)
       │
       └──→ HTTP 200 + JSON
            {
              "id": 1,
              "bookingId": "BOOKING-001",
              "status": "CONFIRMED",
              "createdAt": "2024-01-15T10:30:00"
            }

STEP 6: MS-Payments traite asynchroniquement (EN ARRIÈRE PLAN)
═════════════════════════════════════════════════════════════

Kafka Consumer (MS-Payments)
  │
  └──→ ReservationEventConsumer.consumeReservationCreatedEvent()
       [Appelé automatiquement par Kafka listener]
       │
       └──→ PaymentService.processPaymentFromReservation(event)
            │
            ├─→ Vérifie idempotence (Payment n'existe pas déjà)
            │
            ├─→ Crée Payment entity
            │   ├─ bookingId = event.getBookingId()
            │   ├─ amount = event.getTotalPrice()
            │   ├─ transactionId = "TXN-1705-1030-001"
            │   └─ status = PENDING → COMPLETED
            │
            ├─→ Simule traitement: Thread.sleep(1000)
            │
            └─→ paymentRepository.save(payment)
                [Sauvegarde dans MySQL ms_payments:3308]

STEP 7: Vérification
═════════════════════

Client
  │
  └──→ GET /api/payments/customer/user@example.com
       (après 2-3 secondes)
       │
       └──→ HTTP 200
            [
              {
                "id": 1,
                "bookingId": "BOOKING-001",
                "amount": 450.00,
                "status": "COMPLETED",
                "transactionId": "TXN-1705-1030-001"
              }
            ]
```

---

## 3. Communication Entre Microservices

### 3.1 Communication Synchrone (REST/HTTP)

```
SYNCHRONOUS COMMUNICATION PATTERN
═════════════════════════════════

Période: IMMÉDIATE
Bloquant: OUI (MS-Bookings attend la réponse de MS-Properties)
Tolérance aux pannes: FAIBLE (panne de MS-Properties = panne de MS-Bookings)
Utilisation: Vérifier la disponibilité AVANT de créer une réservation

FLUX:
──────

1. MS-Bookings: BookingService.createBooking()
                Request: PropertyServiceClient

2. RestTemplate client établit une connexion HTTP

3. Envoie GET http://ms-properties:8081/api/properties/PROP-001/available

4. MS-Properties: PropertyController.getAvailableProperty(@PathVariable id)
                  │
                  └──→ PropertyService.getAvailableProperty(id)
                       ├─→ Vérifie disponibilité en DB
                       └─→ Retourne PropertyDTO

5. RestTemplate reçoit la réponse

6. MS-Bookings continue le flux:
   ├─ Si HTTP 200 OK + available: Crée la réservation
   └─ Si HTTP 409 Conflict: Lance PropertyNotAvailableException


RELIABILITY PATTERNS:
─────────────────────

- Timeout: RestTemplate timeout = 5 secondes
- Retry: PropertyServiceClient essaye 3x en cas d'erreur
- Circuit Breaker: (À implémenter avec Resilience4j si nécessaire)
- Fallback: Si MS-Properties est down, lancez ServiceUnavailableException


CODE EN MS-BOOKINGS:
───────────────────

@Service
public class PropertyServiceClient {
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${ms.properties.url}")
    private String propertiesServiceUrl;
    
    public PropertyDTO getAvailableProperty(String propertyId) {
        String url = propertiesServiceUrl + "/properties/" + propertyId + "/available";
        
        try {
            ResponseEntity<PropertyDTO> response = restTemplate.getForEntity(url, PropertyDTO.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                throw new PropertyNotAvailableException(
                    "Property " + propertyId + " is not available"
                );
            }
        } catch (HttpClientErrorException e) {
            throw new PropertyServiceUnavailableException(
                "MS-Properties service is temporarily unavailable", e
            );
        }
        
        return null;
    }
}
```

### 3.2 Communication Asynchrone (Kafka/Events)

```
ASYNCHRONOUS COMMUNICATION PATTERN
══════════════════════════════════

Période: DIFFÉRÉE (après réservation confirmée)
Bloquant: NON (MS-Bookings ne attend pas MS-Payments)
Tolérance aux pannes: ÉLEVÉE (panne de MS-Payments ≠ panne de réservation)
Utilisation: Créer un paiement après une réservation

FLUX:
──────

1. MS-Bookings: Réservation créée et confirmée

2. BookingService appelle:
   ReservationEventProducer.publishReservationCreatedEvent(booking)

3. ReservationEventProducer:
   ├─→ Crée ReservationCreatedEvent (DTO du message)
   ├─→ kafkaTemplate.send("reservations-topic", event)
   └─→ Retour IMMÉDIAT au client (non-bloquant)

4. Kafka stocke le message dans le topic "reservations-topic"
   avec 1 partition et replication factor 1

5. MS-Payments Kafka Consumer:
   ├─→ @KafkaListener(topics = "reservations-topic", groupId = "payments-group")
   ├─→ Automatiquement invoké quand un message arrive
   └─→ ReservationEventConsumer.consumeReservationCreatedEvent(event)

6. MS-Payments:
   PaymentService.processPaymentFromReservation(event)
   ├─→ Traite le paiement (simulation)
   └─→ Sauvegarde le résultat en DB

7. En cas d'erreur:
   ├─→ Log ERROR dans MS-Payments
   ├─→ Message reste dans Kafka (peut être retraité)
   └─→ Ne bloque PAS la réservation


RELIABILITY PATTERNS:
─────────────────────

- Message Ordering: Garantie par partition Kafka
- At-Least-Once Delivery: Consumer group tracking offsets
- Idempotence: PaymentService vérifie que Payment n'existe pas déjà
- Dead Letter Queue: (À implémenter si produit échoue)
- Retries: Kafka retry automatique selon retention policy


AVANTAGES:
───────────

✅ Découplage complet entre MS-Bookings et MS-Payments
✅ Scalabilité: Ajouter 10 consumers n'affecte pas le producteur
✅ Résilience: Pais de dépendances critiques
✅ Asynchrone: Réponse utilisateur immédiate
✅ Audit trail: Tous les événements enregistrés dans Kafka


EVENT SCHEMA:
──────────────

{
  "bookingId": "BOOKING-001-160524-001",
  "propertyId": "PROP-001",
  "customerName": "Jean Dupont",
  "customerEmail": "jean@example.com",
  "checkInDate": "2024-02-01",
  "checkOutDate": "2024-02-05",
  "nights": 4,
  "pricePerNight": 112.50,
  "totalPrice": 450.00,
  "createdAt": "2024-01-15T10:30:00.123Z"
}


TOPIC CONFIGURATION:
────────────────────

Topic: "reservations-topic"
├─ Partitions: 1
├─ Replication Factor: 1
├─ Retention: 7 days
└─ Format: JSON (Spring Kafka JsonSerializer)

Consumer Group: "payments-group"
├─ Offset: Earliest (from start if first time)
├─ Auto Commit: true
└─ Session Timeout: 30 seconds
```

---

## 4. Diagramme des Dépendances

```
EXTERNAL DEPENDENCIES:
═════════════════════

                    ┌──────────────┐
                    │ Docker Host  │
                    │ Network      │
                    └──────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
    ┌────────┐        ┌────────┐        ┌────────┐
    │ Kafka  │────────│ Redis  │────────│ MySQL  │
    │ (9092) │        │(6379)  │        │(3306-8)│
    │        │        │        │        │        │
    │Topic:  │        │  TTL   │        │  3x DB │
    │reserv. │        │ 10 min │        │   Inst │
    └────────┘        └────────┘        └────────┘
        ▲                  ▲                  ▲
        │                  │                  │
    ┌───┴──────────────────┴──────────────────┴────┐
    │                                                │
    │     MICROSERVICES LAYER                      │
    │                                                │
    ├──────────────────────────────────────────────┤
    │                                                │
    │  ┌──────────────┐  ┌──────────────────┐     │
    │  │ MS-PROPERTIES│  │ MS-BOOKINGS      │     │
    │  │ (8081)       │  │ (8082)           │     │
    │  │              │◄─┤ [REST call]      │     │
    │  │ Depends on:  │  │ Depends on:      │     │
    │  │ ├─ MySQL     │  │ ├─ MySQL         │     │
    │  │ ├─ Redis     │  │ ├─ Redis         │     │
    │  │ ├─ Kafka     │  │ ├─ Kafka(pub)    │     │
    │  │ └─ Logback   │  │ ├─ MS-Props      │     │
    │  │              │  │ └─ Logback       │     │
    │  └──────────────┘  │                  │     │
    │                    └──────────────────┘     │
    │                             ▲                │
    │                    ┌────────┴───────┐       │
    │                    │ Kafka Topic    │       │
    │                    │ reservations   │       │
    │                    │ -topic         │       │
    │                    └────────┬───────┘       │
    │                             │               │
    │                    ┌────────▼───────┐       │
    │                    │ MS-PAYMENTS    │       │
    │                    │ (8083)         │       │
    │                    │ [Kafka consume]│       │
    │                    │ Depends on:    │       │
    │                    │ ├─ MySQL       │       │
    │                    │ ├─ Redis       │       │
    │                    │ ├─ Kafka(sub)  │       │
    │                    │ └─ Logback     │       │
    │                    └────────────────┘       │
    │                                                │
    └──────────────────────────────────────────────┘


INTRA-SERVICE DEPENDENCIES:
════════════════════════════

MS-Properties:
──────────────
PropertiesApplication
    │
    ├─→ Controller (PropertyController)
    │       ├─→ Service (PropertyService)
    │       │       ├─→ Repository (PropertyRepository)
    │       │       ├─→ Config (CacheConfig)
    │       │       └─→ Cache annotations
    │       │
    │       └─→ Exception handlers (GlobalExceptionHandler)
    │
    └─→ Database: MySQL on port 3306

MS-Bookings:
─────────────
BookingsApplication
    │
    ├─→ Controller (BookingController)
    │       ├─→ Service (BookingService)
    │       │       ├─→ Repository (BookingRepository)
    │       │       ├─→ PropertyServiceClient [REMOTE CALL]
    │       │       ├─→ ReservationEventProducer [KAFKA PUBLISH]
    │       │       ├─→ Config (KafkaConfig)
    │       │       └─→ Cache annotations
    │       │
    │       └─→ Exception handlers (GlobalExceptionHandler)
    │
    ├─→ Events
    │       ├─→ ReservationCreatedEvent (DTO)
    │       └─→ ReservationEventProducer
    │
    └─→ Database: MySQL on port 3307

MS-Payments:
─────────────
PaymentsApplication
    │
    ├─→ Controller (PaymentController)
    │       ├─→ Service (PaymentService)
    │       │       ├─→ Repository (PaymentRepository)
    │       │       ├─→ ReservationEventConsumer [KAFKA LISTENER]
    │       │       ├─→ Config (KafkaConfig)
    │       │       └─→ Cache annotations
    │       │
    │       └─→ Exception handlers (GlobalExceptionHandler)
    │
    ├─→ Events
    │       ├─→ ReservationCreatedEvent (DTO - same as MSB)
    │       └─→ ReservationEventConsumer
    │
    └─→ Database: MySQL on port 3308
```

---

## 5. Séquence complète: Synchrone + Asynchrone

```
Time    Client                 MS-Bookings            MS-Properties       Kafka              MS-Payments
────    ──────                 ───────────            ─────────────       ─────              ───────────

T0      POST /api/bookings     ◄───────────────────────────────────────────────────────────────────────
        {propertyId, dates}    
                                │
T1                              BookingService.createBooking()
                                │
T2                              PropertyServiceClient.getAvailableProperty()
                                                      ────────────────────────────►
                                                      PropertyController
                                                      PropertyService.checkAvailable()
T3                                                    ◄────────────────────────────
                                                      HTTP 200 OK (available)
                                │
T4                              Save Booking in DB
                                └─► mysql-bookings:3307
                                
T5                              ReservationEventProducer.publish()
                                                                      ────────► reservations-topic
                                                                      (json message enqueued)
T6      HTTP 200 OK            ◄───────────────────────────────────────────────────────────────────
        {Booking: CONFIRMED}  
        [RETURNS IMMEDIATELY - NO WAITING FOR PAYMENT]

                                [ASYNCHRONOUS PROCESSING CONTINUES]
                                
                                                                      Kafka Consumer
T7                                                                  (in background)
                                                                      │
T8                                                                   ──► MS-Payments
                                                                      ReservationEventConsumer
                                                                      │
T9                                                                   PaymentService
                                                                   .processPaymentFromReservation()
                                                                   │
T10                                                                Thread.sleep(1000) [simulate processing]
                                                                   
T11                                                                Save Payment in DB
                                                                   └─► mysql-payments:3308

T12 GET /api/payments/customer/.. ◄────────────────────────────────────────────────────────────────
                                                                   PaymentController
                                ────────► HTTP 200
    {Payments: [COMPLETED]}    ◄────────────────────────────────────────────────────────────────···


TIME BREAKDOWN (from request to payment completion):
══════════════════════════════════════════════════════

T0-T6:   SYNCHRONOUS part (Blocking)
         - Property check: ~50ms
         - Save booking: ~100ms
         - Kafka publish: ~50ms
         - TOTAL: ~200ms
         - CLIENT SEES: Confirmation at T6

T6-T12:  ASYNCHRONOUS part (Non-blocking)
         - Kafka consume lag: ~100ms
         - Payment processing: ~1000ms (Thread.sleep)
         - Save payment: ~100ms
         - TOTAL: ~1200ms
         - CLIENT DOES NOT WAIT: Already got response at T6


KEY INSIGHTS:
═════════════

✓ User gets booking confirmation in ~200ms
✓ User doesn't wait for payment processing
✓ Payment gets created asynchronously in ~1200ms
✓ If MS-Payments is down, booking is still successful
✓ If MS-Properties is down, booking fails fast (fails immediately, doesn't hang)
```

---

## 6. Cache Strategy Diagram

```
CACHE IN MS-PROPERTIES:
════════════════════════

GET /api/properties/1
    │
    ▼
PropertyService.getPropertyById(1)
    │
    ┌─────────────────────────────┐
    │ @Cacheable("properties")    │
    └────────────────┬────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
    Cache HIT?              Cache MISS?
    (Redis)                 (Database)
        │                         │
        ├─ YES                    ├─ NO
        │   Return cached         │   Query MySQL
        │   PropertyDTO           │   (SELECT * FROM properties WHERE id=1)
        │   [~1ms]                │   [~50ms]
        │                         │
        │   ┌─────────────────────┘
        │   │
        └───┼──────────────────┐
            │                  ▼
            │            @CacheEvict?
            │            (on update/delete)
            │                  │
            │                  ├─ YES (UPDATE or DELETE property)
        Return to             │   Clear cache entry
        PropertyDTO           │   "properties::1"
                              │   in Redis
                              │
                              └──► Invalidation Complete
                                   Next GET will hit DB


CACHE CONFIGURATION:
════════════════════

Redis:
├─ Host: redis (docker-compose)
├─ Port: 6379
├─ TTL: 10 minutes (600 seconds)
└─ Policy: LRU (evict least recently used if memory full)

Cache Names:
├─ "properties"           (All property queries)
├─ "properties-by-city"   (City-filtered queries)
├─ "bookings"             (All booking queries)
├─ "bookings-by-email"    (Email-filtered queries)
├─ "payments"             (All payment queries)
└─ "payments-by-email"    (Email-filtered queries)


ANNOTATIONS:
═════════════

@Cacheable(value = "properties")
├─ Condition: Result goes into cache if not already there
├─ Key generation: Auto from method params
└─ Example: @Cacheable("properties") on getPropertyById(id)
           └─ Key: "properties::1"

@CacheEvict(value = "properties", allEntries = true)
├─ Condition: Clear entire cache when method executes
├─ When: On create, update, or delete
└─ Example: When createProperty() succeeds, clear all property cache

@CachePut(value = "properties", key = "#id")
├─ Condition: Always execute method & update cache
├─ Use when: Method result should always update cache
└─ Example: When updating property details
```

---

## 7. Error Handling Flow

```
ERROR HANDLING IN MICROSERVICES:
════════════════════════════════

1. VALIDATION ERROR:
   ────────────────

   Client Request
       │
       ▼
   DTO Validation (@NotBlank, @Email, @Positive, etc.)
       │
       ├─ INVALID
       │   │
       │   ▼
       │   ValidationException thrown
       │   │
       │   ▼
       │   GlobalExceptionHandler.handleValidation()
       │   │
       │   └─→ HTTP 400 Bad Request
       │       {
       │         "timestamp": "2024...",
       │         "status": 400,
       │         "error": "Validation Failed",
       │         "message": "Field 'email' is required"
       │       }
       │
       └─ VALID
           │
           ▼
           Process Request

2. BUSINESS LOGIC ERROR:
   ────────────────────

   BookingService.createBooking()
       │
       ├─→ Check if Property available
       │   └─ PropertyNotAvailableException
       │       │
       │       ▼
       │       GlobalExceptionHandler.handlePropertyNotAvailable()
       │       │
       │       └─→ HTTP 409 Conflict
       │           {
       │             "status": 409,
       │             "error": "Property Not Available"
       │           }
       │
       └─ OK
           │
           ▼
           Continue

3. RESOURCE NOT FOUND:
   ──────────────────

   GET /api/properties/999
       │
       ▼
   PropertyService.getPropertyById(999)
       │
       └─→ propertyRepository.findById(999) returns Optional.empty()
           │
           ▼
           throw PropertyNotFoundException(...)
           │
           ▼
           GlobalExceptionHandler.handlePropertyNotFound()
           │
           └─→ HTTP 404 Not Found
               {
                 "status": 404,
                 "error": "Property Not Found",
                 "message": "Property with id 999 not found"
               }

4. EXTERNAL SERVICE ERROR (Synchronous Call):
   ─────────────────────────────────────────

   PropertyServiceClient.getAvailableProperty()
       │
       └─→ RestTemplate.getForEntity() throws exception
           ├─ ConnectionTimeout
           ├─ ReadTimeout
           ├─ HttpServerErrorException (500)
           └─ HttpClientErrorException
               │
               ▼
               catch block
               │
               ▼
               throw PropertyServiceUnavailableException(...)
               │
               ▼
               GlobalExceptionHandler.handleServiceUnavailable()
               │
               └─→ HTTP 503 Service Unavailable
                   {
                     "status": 503,
                     "error": "Service Unavailable",
                     "message": "MS-Properties service is temporarily unavailable"
                   }

5. KAFKA CONSUMER ERROR (Asynchronous):
   ──────────────────────────────────

   ReservationEventConsumer.consumeReservationCreatedEvent()
       │
       ├─→ PaymentService.processPaymentFromReservation()
       │   │
       │   └─→ Exception during processing
       │       │
       │       ▼
       │       catch (Exception e)
       │       {
       │           log.error("Error processing event for booking: {}", 
       │               event.getBookingId(), e);
       │           // NO THROW - Don't let consumer fail
       │           // Kafka will retry based on retention policy
       │       }
       │
       └─ Booking remains CONFIRMED (async error doesn't block)
          Payment creation may retry later

6. DATABASE ERROR:
   ───────────────

   BookingService.createBooking()
       │
       └─→ bookingRepository.save(booking) throws
           ├─ DataIntegrityViolationException (duplicate key)
           ├─ HibernateException (mapping error)
           └─ SQLException
               │
               ▼
               catch + log + throw ApplicationException
               │
               ▼
               GlobalExceptionHandler.handleException()
               │
               └─→ HTTP 500 Internal Server Error


ERROR RESPONSE FORMAT (Standardized):
═════════════════════════════════════

{
  "timestamp": "2024-01-15T10:30:05.123456Z",
  "status": 400 | 404 | 409 | 500 | 503,
  "error": "Validation Failed | Not Found | Conflict | Internal Server Error",
  "message": "Human readable explanation",
  "path": "/api/resources/123"
}
```

---

## 8. Data Flow & Storage Architecture

```
DATA PERSISTENCE STRATEGY:
══════════════════════════

PATTERN: Database Per Service (Microservices Pattern)

Each service has:
├─ Own database (MySQL instance)
├─ Own cache (Redis - shared)
└─ No shared/central database


MS-PROPERTIES Data:
────────────────

MySQL (port 3306, database: ms_properties)
│
├─ TABLES:
│  └─ properties
│     ├─ id (PK)
│     ├─ code (UNIQUE, indexed)
│     ├─ title
│     ├─ description
│     ├─ location
│     ├─ price_per_night (DECIMAL)
│     ├─ total_bedrooms
│     ├─ total_bathrooms
│     ├─ available (BOOLEAN)
│     ├─ owner_name
│     ├─ owner_email
│     ├─ created_at (TIMESTAMP)
│     └─ updated_at (TIMESTAMP)
│
└─ INDICES:
   ├─ PRIMARY KEY (id)
   ├─ UNIQUE (code)
   └─ Regular (location, available)


MS-BOOKINGS Data:
─────────────────

MySQL (port 3307, database: ms_bookings)
│
├─ TABLES:
│  └─ bookings
│     ├─ id (PK)
│     ├─ booking_id (UNIQUE, indexed)
│     ├─ property_id (FK concept - denormalized)
│     ├─ customer_name
│     ├─ customer_email (indexed)
│     ├─ check_in_date
│     ├─ check_out_date
│     ├─ status (ENUM: PENDING, CONFIRMED, CANCELLED, COMPLETED)
│     ├─ created_at (TIMESTAMP)
│     └─ updated_at (TIMESTAMP)
│
└─ INDICES:
   ├─ PRIMARY KEY (id)
   ├─ UNIQUE (booking_id)
   └─ Regular (customer_email, status)


MS-PAYMENTS Data:
─────────────────

MySQL (port 3308, database: ms_payments)
│
├─ TABLES:
│  └─ payments
│     ├─ id (PK)
│     ├─ booking_id (FK concept - denormalized)
│     ├─ amount (DECIMAL)
│     ├─ status (ENUM: PENDING, COMPLETED, FAILED)
│     ├─ transaction_id (UNIQUE, indexed)
│     ├─ customer_email
│     ├─ created_at (TIMESTAMP)
│     └─ updated_at (TIMESTAMP)
│
└─ INDICES:
   ├─ PRIMARY KEY (id)
   ├─ UNIQUE (booking_id)
   ├─ UNIQUE (transaction_id)
   └─ Regular (customer_email, status)


CACHE LAYER:
─────────────

Redis (port 6379) - SHARED across all services
│
├─ Key patterns:
│  ├─ properties::1 (PropertyDTO for id=1)
│  ├─ properties::*
│  ├─ properties-by-city::Paris
│  ├─ bookings::1 (BookingDTO for id=1)
│  ├─ bookings::*
│  ├─ bookings-by-email::user@example.com
│  ├─ payments::1
│  ├─ payments::*
│  └─ payments-by-email::user@example.com
│
├─ TTL: 600 seconds (10 minutes) for all keys
└─ Eviction: LRU (Least Recently Used)


DATA FLOW TO CACHE:
────────────────────

Request
  │
  ▼
  ┌──────────────────┐
  │ Check in Cache   │
  │ (Redis)          │
  └────────┬─────────┘
           │
        ┌──┴──┐
        │     │
      MISS   HIT
        │     │
        ▼     ▼
      MySQL  Return Cached
        │     Data
        │     │
        ├─────┤
        │     │
        ▼     ▼
      Write Cache with TTL
        │
        ▼
      Return to Client


INVALIDATION STRATEGY:
──────────────────────

CREATE: @CacheEvict(allEntries=true)
        └─ Clear all cache for that entity type

UPDATE: @CacheEvict(allEntries=true)
        └─ Clear all cache entries

DELETE: @CacheEvict(allEntries=true)
        └─ Clear all cache entries

TTL Expiration: Automatic after 10 minutes via Redis
```

---

## 9. Technology Stack Visualization

```
PRESENTATION LAYER:
═══════════════════
┌──────────────────┐
│ REST Controllers │
│ @RestController  │
│ @RequestMapping  │
└──────────────────┘

BUSINESS LOGIC LAYER:
════════════════════
┌──────────────────┐     ┌──────────────────┐
│  Services        │────▶│  Event Producer  │
│  @Service        │     │  Kafka Send      │
│  @Transactional  │     │                  │
└──────────────────┘     └──────────────────┘

┌──────────────────┐     ┌──────────────────┐
│ Service Clients  │────▶│ REST Template    │
│ HTTP Outbound    │     │ (Sync Calls)     │
└──────────────────┘     └──────────────────┘

PERSISTENCE LAYER:
═══════════════════
┌──────────────────┐
│ JPA Repositories │
│ @Repository      │
│ JpaRepository<T> │
└────────┬─────────┘
         │
    ┌────▼───────────────┐
    │                    │
    ▼                    ▼
  MySQL 8            Hibernate ORM
  (Database)         (Mapping)

CACHING LAYER:
═══════════════
┌──────────────────┐
│   Spring Cache   │
│ @Cacheable       │
│ @CacheEvict      │
└────────┬─────────┘
         │
         ▼
    Redis 7
    (In-Memory Store)

MESSAGE LAYER:
═══════════════
┌──────────────────┐     ┌──────────────────┐
│ Kafka Producer   │────▶│ Kafka Topic      │
│ @KafkaListener   │     │ reservations-    │
│ KafkaTemplate    │     │ topic            │
└──────────────────┘     └──────────────────┘
         ▲
         │
┌────────┴─────────────┐
│                      │
│ Kafka Consumer       │
│ @KafkaListener       │
│ (MS-Payments)        │
└──────────────────────┘


FRAMEWORK & LIBRARIES:
═════════════════════

Core:
├─ Spring Boot 2.7.14
├─ Spring Framework 5.3.x
├─ Spring Data JPA
├─ Spring Kafka
├─ Spring Cache
└─ Spring Web

Database:
├─ MySQL 8.0
├─ Hibernate ORM 5.6
├─ MySQL JDBC Driver 8.0
└─ Jedis (Redis client)

Infrastructure:
├─ Apache Kafka 7.5.0
├─ Zookeeper 7.5.0
├─ Redis 7.0
└─ Docker 24.0

Utilities:
├─ Lombok
├─ Jackson (JSON)
├─ SLF4J / Logback
└─ Bean Validation (JSR-380)
```

---

## 10. Monitoring & Observability

```
MONITORING POINTS:
═════════════════

Application Health:
├─ HTTP 200 → /actuator/health
├─ Responds with UP/DOWN status
├─ Components checked:
│  ├─ Database connectivity
│  ├─ Redis availability
│  └─ Kafka broker status
└─ Executed every 30 seconds

Metrics Exposed:
├─ /actuator/metrics
├─ JVM memory usage
├─ Request count/latency
├─ Database connection pool
├─ Kafka consumer lag
└─ Cache hit/miss rate

Logging:
├─ File: logs/{service}.log
├─ Level: DEBUG for custom classes, INFO for frameworks
├─ Pattern: timestamp [thread] level class message
├─ Rotation: Daily, 30-day retention
└─ Key events logged:
    ├─ HTTP requests received
    ├─ Service methods entry/exit
    ├─ Cache hits/misses
    ├─ Kafka events published/received
    ├─ Database operations
    └─ Exceptions & errors


OBSERVABILITY STACK:
═════════════════════

┌────────────────┐
│ Services Logs  │
│  (files)       │
└────────┬───────┘
         │
         ▼
    ┌─────────────┐
    │ Aggregation │
    │ (manually)  │
    └────────┬────┘
             │
         ┌───▼──────┬────────┬──────────┐
         │           │        │          │
         ▼           ▼        ▼          ▼
    Console    Docker logs  Files   Metrics
    stdout     CLI          ~/logs/  /actuator


DOCKER COMPOSE OBSERVABILITY:
════════════════════════════

docker-compose logs ms-bookings
├─ Real-time logs from specific service
├─ Shows STDOUT + file logs
└─ Useful for debugging

docker-compose logs -f
├─ Follow mode (tail)
├─ All services' logs at once
└─ Essential during testing

docker ps
├─ Show running containers
├─ Status (Up, Exited, etc.)
└─ Health status if configured


EXAMPLE: TRACING A REQUEST
════════════════════════════

1. User sends: POST /api/bookings
   └─ Log in ms-bookings:
      [BookingController] POST /api/bookings received

2. BookingService.createBooking()
   └─ Log: [BookingService] Creating booking for property PROP-001

3. PropertyServiceClient sync call
   └─ Log: [PropertyServiceClient] Calling MS-Properties at ...

4. MS-Properties receives call
   └─ Log in ms-properties:
      [PropertyController] GET /api/properties/PROP-001/available
      [PropertyService] Checking availability for property PROP-001
      [PropertyRepository] Query: SELECT * FROM properties WHERE id=...

5. Response returns
   └─ Log: [PropertyController] Property PROP-001 available: true

6. Booking saved
   └─ Log: [BookingService] Booking saved with ID BOOKING-001

7. Kafka publish
   └─ Log: [EventProducer] Published event BOOKING-001 to Kafka

8. Response to user
   └─ Log: [BookingController] Returning HTTP 200 to client
```

---

## Reference Links

- [Domain-Driven Design Patterns](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [Microservices Communication](https://microservices.io/patterns/communication-style/messaging.html)
- [Event Sourcing & Event Streaming](https://kafka.apache.org/documentation/#introduction)
- [Spring Microservices](https://spring.io/projects/spring-cloud)

---

**Document Version:** 1.0.0  
**Last Updated:** 2024-01-15  
**Status:** Complete
