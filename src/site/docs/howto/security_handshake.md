# Security Handshake

All endpoints that can mutate the database require a header of `Authorization: Bearer <jwt_token>`. You can obtain a JWT token by submitting a `POST /auth` request such as the following:

```text
POST /anno/v1/auth
Authorization: APIKEY <your_api_key>
```

Example using curl:

```bash
curl -X 'POST' \
  'http:/myserver.org/anno/v1/auth' \
  -H 'Authorization: APIKEY <your_api_key>'
```

## Security flow diagram

```mermaid
sequenceDiagram
    actor U as User
    participant A as Annosaurus
    U->>+A: GET /auth
    activate A
    Note right of A: Authorization: APIKEY <your api key>
    A-->>U: {..., "access_token": <jwt> }
    deactivate A
    U->>+A: POST/DELETE/PUT
    activate A
    Note right of A: Authorization: Bearer <jwt>
    A-->>U: Success reponse 20x
    deactivate A
```
