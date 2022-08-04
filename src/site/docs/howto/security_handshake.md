# Security Handshake

All endpoints that can mutate the database require a header of `Authorization: Bearer <jwt_token>`. You can obtain a JWT token by submitting a `POST /auth` request like the following:

```text


```

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