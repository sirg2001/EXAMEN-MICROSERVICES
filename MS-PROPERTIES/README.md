# MS-Properties - Microservice de Gestion des Propriétés

## 📋 Description

Ce microservice gère les propriétés immobilières dans une architecture microservices. Il offre des endpoints pour créer, consulter, mettre à jour et supprimer des propriétés.

## 🏗️ Architecture

```
PropertyController
       ↓
PropertyService (avec Caching + Logging)
       ↓
PropertyRepository
       ↓
MySQL Database (ms_properties)

Redis: Caching des propriétés
```

## 🚀 Démarrage Local

### Prérequis
- Java 11+
- Maven 3.8+
- Docker et Docker Compose
- Git

### Mode Local (Sans Docker)

#### 1. Démarrer les dépendances
```bash
# MySQL
docker run -d \
  --name mysql_properties \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=ms_properties \
  -p 3306:3306 \
  mysql:8

# Redis
docker run -d \
  --name redis_properties \
  -p 6379:6379 \
  redis:7-alpine
```

#### 2. Lancer le service
```bash
mvn clean install
mvn spring-boot:run
```

Service écoute sur: **http://localhost:8081/api**

### Mode Docker Compose (Recommandé)

```bash
# Démarrer
docker-compose up -d

# Vérifier l'état
docker-compose ps

# Logs
docker-compose logs -f ms-properties

# Arrêter
docker-compose down
```

## 📡 Endpoints API

### Récupérer une propriété
```bash
GET /api/properties/{id}

Réponse:
{
  "id": 1,
  "title": "Villa avec piscine",
  "description": "Belle villa à Dakar",
  "address": "123 Rue de la Paix",
  "city": "Dakar",
  "zipCode": 18000,
  "area": 250.0,
  "bedrooms": 4,
  "bathrooms": 2,
  "pricePerNight": 150.00,
  "available": true,
  "ownerName": "John Doe",
  "ownerEmail": "john@example.com",
  "ownerPhone": "+221981234567"
}
```

### Vérifier la disponibilité (Utilisé par MS-Bookings)
```bash
GET /api/properties/{id}/available

# Retourne la propriété si disponible, sinon erreur 404
```

### Créer une propriété
```bash
POST /api/properties
Content-Type: application/json

{
  "title": "Appartement moderne",
  "description": "Appartement 3 pièces",
  "address": "456 Avenue de la Mer",
  "city": "Dakar",
  "zipCode": 18000,
  "area": 120.0,
  "bedrooms": 2,
  "bathrooms": 1,
  "pricePerNight": 80.00,
  "ownerName": "Jane Smith",
  "ownerEmail": "jane@example.com",
  "ownerPhone": "+221987654321"
}
```

### Mettre à jour une propriété
```bash
PUT /api/properties/{id}
Content-Type: application/json

{
  "title": "Titre modifié",
  "pricePerNight": 100.00,
  ...
}
```

### Supprimer une propriété
```bash
DELETE /api/properties/{id}
```

### Récupérer les propriétés disponibles
```bash
GET /api/properties/available

Retourne une liste de toutes les propriétés disponibles
```

### Récupérer les propriétés en une ville
```bash
GET /api/properties/city/{city}/available

Exemple: GET /api/properties/city/Dakar/available
```

### Changer la disponibilité
```bash
PATCH /api/properties/{id}/availability?available=false

Change la disponibilité d'une propriété
```

## 🔍 Tests

### Avec cURL

```bash
# Créer une propriété
curl -X POST http://localhost:8081/api/properties \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Villa",
    "description": "Description",
    "address": "123 Street",
    "city": "Dakar",
    "zipCode": 18000,
    "area": 250,
    "bedrooms": 4,
    "bathrooms": 2,
    "pricePerNight": 150,
    "ownerName": "Owner",
    "ownerEmail": "owner@example.com",
    "ownerPhone": "+221981234567"
  }'

# Récupérer une propriété
curl http://localhost:8081/api/properties/1

# Récupérer disponibles
curl http://localhost:8081/api/properties/available
```

### Avec Postman/Insomnia
Importer les endpoints depuis les exemples ci-dessus.

## 🔄 Communication avec d'autres services

### Avec MS-Bookings (Synchrone)

MS-Bookings appelle MS-Properties pour vérifier la disponibilité avant de créer une réservation:

```
GET http://ms-properties:8081/api/properties/{propertyId}/available
```

Réponse: Le détail de la propriété ou erreur 404

## 📊 Caching (Redis)

- Les propriétés récupérées via `GET /{id}` sont cachées
- TTL: 10 minutes
- Le cache est invalidé lors de PUT, DELETE, PATCH

**Vérifier le cache:**
```bash
redis-cli
> KEYS *
> GET "properties::1"
```

## 📝 Logging

Tous les accès et erreurs sont loggés:

```
2026-03-19 10:30:45 - com.realestate.properties.service.PropertyService - INFO - Fetching property from database: 1
2026-03-19 10:30:46 - com.realestate.properties.service.PropertyService - ERROR - Property not found: 999
```

Fichier de logs: `logs/ms-properties.log`

## 🏗️ Structure du Projet

```
MS-PROPERTIES/
├── src/
│   ├── main/
│   │   ├── java/com/realestate/properties/
│   │   │   ├── PropertiesApplication.java
│   │   │   ├── controller/
│   │   │   │   └── PropertyController.java
│   │   │   ├── service/
│   │   │   │   └── PropertyService.java
│   │   │   ├── repository/
│   │   │   │   └── PropertyRepository.java
│   │   │   ├── entity/
│   │   │   │   └── Property.java
│   │   │   ├── dto/
│   │   │   │   └── PropertyDTO.java
│   │   │   ├── config/
│   │   │   │   └── CacheConfig.java
│   │   │   └── exception/
│   │   │       ├── PropertyNotFoundException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── .gitlab-ci.yml
├── pom.xml
└── README.md
```

## 🚨 Code de Réponse HTTP

- `200 OK` - Succès
- `201 Created` - Création réussie
- `204 No Content` - Suppression réussie
- `400 Bad Request` - Erreur de validation
- `404 Not Found` - Propriété introuvable
- `500 Internal Server Error` - Erreur serveur

## 🔐 Variables d'Environnement

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ms_properties
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

## 📦 Dépendances Principales

- Spring Boot 2.7.14
- Spring Data JPA
- MySQL Driver
- Spring Data Redis
- Spring Kafka
- Lombok
- Validation (JSR-380)

## 👥 Auteur

Examen Master 2 - Gestion Immobilière

## 📅 Dates Importantes

- Examen: 21 Mars 2026
- Statut: En développement

---

✅ Service maintenant prêt pour la communication avec les autres microservices!
