# tracking-service

Ce microservice est responsable de la **collecte et de la centralisation des événements applicatifs** (audit, traçabilité et sécurité) du prototype Max It. Il stocke les données dans une base de données NoSQL flexible (MongoDB).

## ⚙️ Rôle et Fonctionnalités

- **Collecte universelle** (`/tracking/event`) :
  - Reçoit des événements de tracking standardisés depuis n'importe quel microservice (authentification, transactions, modifications de profils, etc.).
  - Les événements incluent le type d'événement, le numéro de téléphone (msisdn), l'ID de l'utilisateur, son rôle, le service émetteur, un payload libre (contenant des détails spécifiques), et un horodatage.
- **Stockage NoSQL** : Utilise MongoDB (Atlas) pour stocker les documents de tracking de manière non structurée, ce qui facilite l'enregistrement de payloads très variés selon l'action réalisée (connexion, achat, transfert).
- **Sécurité et Contrôle d'Accès** :
  - L'accès à la liste globale des événements est strictement réservé aux administrateurs.
  - Un client standard ne peut consulter que l'historique d'événements lié à son propre numéro de téléphone.

---

## 🔌 Configuration et Endpoints

- **Port par défaut** : `8501`
- **Base de données** : MongoDB (Atlas), configurée via la variable d'environnement `SPRING_DATA_MONGODB_URI`.
- **Technologie** : Spring Boot, Spring Data MongoDB, Netflix Eureka Client

### Endpoints exposés :

#### 1. Collecte d'un événement (Usage Interne)
* **URL** : `POST /tracking/event`
* **Corps de la requête (JSON)** :
  ```json
  {
    "eventType": "ACHAT_PASS_INTERNET",
    "msisdn": "771234567",
    "userId": "5",
    "userRole": "CLIENT",
    "sourceService": "pricing-service",
    "payload": {
      "passId": 2,
      "prix": 2000.0,
      "receveur": "771234567"
    },
    "timestamp": "2026-07-10T07:49:00Z"
  }
  ```
* **Réponse (201 Created)** : Renvoie l'événement enregistré avec son ID MongoDB généré.

#### 2. Consulter tous les événements (Administrateur uniquement)
* **URL** : `GET /tracking/events`
* **En-têtes requis** : `X-User-Role` (doit être égal à `ADMINISTRATOR`).

#### 3. Consulter les événements par client
* **URL** : `GET /tracking/events/{msisdn}`
* **Règles de sécurité** :
  - Un utilisateur possédant le rôle `CLIENT` ne peut consulter que ses propres événements. S'il tente de renseigner un autre numéro dans l'URL, le service renvoie une erreur `403 Forbidden`.
  - Les administrateurs peuvent interroger n'importe quel numéro.
