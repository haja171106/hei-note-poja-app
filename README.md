# HEI Note - Guide de Test des APIs REST (Hors Interface Graphique)

Ce guide détaille comment tester les fonctionnalités clés de l'application **HEI Note** directement via des requêtes HTTP (via `curl`, Postman, Insomnia, etc.), sans passer par l'interface utilisateur graphique.

---

## Informations Générales

- **URL de Déploiement** : `https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws`
- **Format des échanges** : JSON (`Content-Type: application/json`, `Accept: application/json`)
- **Authentification** : JWT (JSON Web Token) transmis via le header `Authorization: Bearer <TOKEN>`

> ⚠️ **Remarque Importante sur les Données d'Exemple :**
> Toutes les données fournies dans ce document (identifiants UUID, noms, emails, mots de passe, tokens JWT) sont données **uniquement à titre d'exemple et d'illustration**. Elles n'existent pas réellement en base de données.
> Pour tester réellement l'API en production, vous devez disposer des identifiants d'un **compte administrateur réel** enregistré dans la base de données afin de vous authentifier, récupérer les vrais identifiants (promotions, étudiants, groupes, etc.) et exécuter les requêtes.

---

## Étape Préliminaire : Obtenir le Token JWT (Authentification)

Toutes les routes protégées nécessitent un jeton JWT valide.

### Endpoint de Connexion
- **Méthode** : `POST`
- **URL** : `/auth/login`
- **Authentification requise** : Aucune (Public)

#### Corps de la requête (JSON) :
```json
{
  "email": "hei.admin@admin.com",
  "password": "Password123!"
}
```

> **Règle des adresses email HEI :**
> - **ADMIN** : format `hei.<prenom>@admin.com`
> - **TEACHER** : format `hei.<prenom>@teacher.com`
> - **STUDENT** : format `hei.<prenom>@student.com`

#### Exemple cURL :
```bash
curl -X POST https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "hei.admin@admin.com",
    "password": "Password123!"
  }'
```

#### Exemple de Réponse (200 OK) :
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "ADMIN",
  "userId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "expiresAt": "2026-08-20T22:00:00Z"
}
```

>  **Astuce** : Copiez la valeur du champ `"token"` et dupliquez-la dans le header `Authorization: Bearer <TOKEN>` pour les requêtes suivantes.

---

##  Guide des Fonctionnalités à Tester

---

### 1. Récupérer la liste des étudiants (ADMIN)

Permet à un administrateur de lister tous les étudiants inscrits dans une promotion (cohorte).

- **Méthode** : `GET`
- **URL** : `/promotions/{cohortId}/students`
- **Rôle requis** : `ADMIN`
- **Paramètres d'URL** :
  - `{cohortId}` *(UUID)* : Identifiant unique de la promotion (ex: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`).

#### Exemple cURL :
```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/promotions/a1b2c3d4-e5f6-7890-abcd-ef1234567890/students \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Accept: application/json"
```

#### Exemple de Réponse (200 OK) :
```json
[
  {
    "id": "e4b1a111-2222-3333-4444-555566667777",
    "ref": "STD23001",
    "name": "Rakoto",
    "firstname": "Jean",
    "email": "hei.jean@student.com",
    "status": "ACTIVE",
    "cohortId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "cohortRef": "A",
    "groupId": "f8c2b222-3333-4444-5555-666677778888",
    "groupRef": "A1",
    "track": null
  }
]
```

---

### 2. Un étudiant récupère toutes ses notes (STUDENT)

Permet à un étudiant connecté de consulter l'ensemble de ses notes par matière.

- **Méthode** : `GET`
- **URL** : `/students/{studentId}/grades`
- **Rôle requis** : `STUDENT` *(le `studentId` doit obligatoirement être celui de l'étudiant connecté, sinon une erreur `403 Forbidden` est retournée)*
- **Paramètres optionnels (Query Params)** :
  - `academicYear` *(Integer, ex: `2023`)* : filtrer par année académique.

#### Exemple cURL :
```bash
curl -X GET "https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/grades?academicYear=2023" \
  -H "Authorization: Bearer <TOKEN_STUDENT>" \
  -H "Accept: application/json"
```

#### Exemple de Réponse (200 OK) :
```json
{
  "studentId": "e4b1a111-2222-3333-4444-555566667777",
  "academicYear": 2023,
  "courses": [
    {
      "courseId": "11111111-2222-3333-4444-555555555555",
      "courseRef": "PROG1",
      "courseTitle": "Algorithmique et Programmation",
      "credit": 30,
      "finalGrade": 14.5
    },
    {
      "courseId": "22222222-3333-4444-5555-666666666666",
      "courseRef": "WEB1",
      "courseTitle": "Développement Web",
      "credit": 30,
      "finalGrade": 16.0
    }
  ]
}
```

---

### 3. Récupérer les notes d'un étudiant (ADMIN)

Un administrateur a un accès global et peut consulter les notes de **n'importe quel étudiant** sans restriction d'identifiant.

- **Méthode** : `GET`
- **URL** : `/students/{studentId}/grades`
- **Rôle requis** : `ADMIN`

#### Exemple cURL (Notes actuelles) :
```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/grades \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Accept: application/json"
```

#### Optionnel — Consulter l'historique des modifications de notes :
L'administrateur peut également consulter l'audit complet des modifications de notes de l'étudiant (qui a modifié la note, quand, ancienne valeur, nouvelle valeur et motif) :
- **Méthode** : `GET`
- **URL** : `/students/{studentId}/grades/history`
- **Rôle requis** : `ADMIN` (ou `STUDENT` pour son propre historique)

```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/grades/history \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Accept: application/json"
```

---

### 4. Changer le groupe d'un étudiant (ADMIN)

Permet à un administrateur de réassigner un étudiant à un autre groupe à tout moment de son parcours (avec conservation de l'historique).

- **Méthode** : `PATCH`
- **URL** : `/students/{studentId}/group`
- **Rôle requis** : `ADMIN`
- **Headers requis** :
  - `Authorization: Bearer <TOKEN_ADMIN>`
  - `Content-Type: application/json`

#### Corps de la requête (JSON) :
```json
{
  "groupId": "99998888-7777-6666-5555-444433332222"
}
```

#### Exemple cURL :
```bash
curl -X PATCH https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/group \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": "99998888-7777-6666-5555-444433332222"
  }'
```

#### Exemple de Réponse (200 OK) :
```json
{
  "id": "e4b1a111-2222-3333-4444-555566667777",
  "ref": "STD23001",
  "name": "Rakoto",
  "firstname": "Jean",
  "email": "hei.jean@student.com",
  "status": "ACTIVE",
  "cohortId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "cohortRef": "A",
  "groupId": "99998888-7777-6666-5555-444433332222",
  "groupRef": "A2",
  "track": null
}
```

---

### 5. Avoir les notes chaque année : Note finale par matière & Moyenne générale (Year Summary)

Calcule et retourne le relevé annuel d'un étudiant pour une année donnée (L1 = Année 1, L2 = Année 2, L3 = Année 3) :
- La liste des matières avec leurs **crédits** et leurs **notes finales** respectives.
- La **moyenne générale pondérée** de l'année.
- Le total des crédits et le statut (`COMPLETE` ou `PROVISIONAL`).

- **Méthode** : `GET`
- **URL** : `/students/{studentId}/years/{year}/summary`
- **Rôles autorisés** :
  - `STUDENT` (pour son propre `studentId`)
  - `ADMIN` (pour tout étudiant)
  - `TEACHER` (résumé partiel des cours qu'il enseigne)
- **Paramètres d'URL** :
  - `{studentId}` *(UUID)* : Identifiant de l'étudiant.
  - `{year}` *(Integer, `1`, `2` ou `3`)* : L'année d'études (ex: `1` pour L1, `2` pour L2, `3` pour L3).

#### Exemple cURL (Année 1 / L1) :
```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/years/1/summary \
  -H "Authorization: Bearer <TOKEN_ADMIN_OU_STUDENT>" \
  -H "Accept: application/json"
```

#### Exemple cURL (Année 2 / L2) :
```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/years/2/summary \
  -H "Authorization: Bearer <TOKEN_ADMIN_OU_STUDENT>" \
  -H "Accept: application/json"
```

#### Exemple cURL (Année 3 / L3) :
```bash
curl -X GET https://ykc5vnpsxpkahbcvse6pnvowt40mupoe.lambda-url.eu-west-3.on.aws/students/e4b1a111-2222-3333-4444-555566667777/years/3/summary \
  -H "Authorization: Bearer <TOKEN_ADMIN_OU_STUDENT>" \
  -H "Accept: application/json"
```

#### Exemple de Réponse (200 OK) :
```json
{
  "studentId": "e4b1a111-2222-3333-4444-555566667777",
  "year": 1,
  "courses": [
    {
      "courseId": "11111111-2222-3333-4444-555555555555",
      "courseRef": "PROG1",
      "courseTitle": "Algorithmique et Programmation",
      "credit": 30,
      "finalGrade": 14.5
    },
    {
      "courseId": "22222222-3333-4444-5555-666666666666",
      "courseRef": "WEB1",
      "courseTitle": "Développement Web",
      "credit": 30,
      "finalGrade": 15.5
    }
  ],
  "overallAverage": 15.0,
  "totalCredits": 60,
  "status": "COMPLETE"
}
```

---

## Tableau Récapitulatif des Endpoints

| Fonctionnalité | Méthode | URL | Rôle(s) Requis |
| :--- | :---: | :--- | :---: |
| **Authentification** | `POST` | `/auth/login` | *Public* |
| **1. Liste des étudiants d'une promo** | `GET` | `/promotions/{cohortId}/students` | `ADMIN` |
| **2. Notes d'un étudiant (par lui-même)** | `GET` | `/students/{studentId}/grades` | `STUDENT` (soi-même) |
| **3. Notes d'un étudiant (par Admin)** | `GET` | `/students/{studentId}/grades` | `ADMIN` |
| **Historique des notes** | `GET` | `/students/{studentId}/grades/history` | `ADMIN`, `STUDENT` |
| **4. Changer le groupe d'un étudiant** | `PATCH` | `/students/{studentId}/group` | `ADMIN` |
| **5. Résumé annuel (Notes + Moyenne)** | `GET` | `/students/{studentId}/years/{year}/summary` | `ADMIN`, `STUDENT`, `TEACHER` |

---

## Codes d'Erreurs Fréquents

- `401 Unauthorized` : Token JWT absent, expiré ou identifiants incorrects au login.
- `403 Forbidden` : Token valide mais rôle insuffisant (ou un étudiant tentant de lire les notes d'un autre étudiant).
- `404 Not Found` : Ressource introuvable (mauvais ID d'étudiant, de cohorte ou de groupe).
- `400 Bad Request` : Corps de requête invalide ou année non comprise entre 1 et 3.
