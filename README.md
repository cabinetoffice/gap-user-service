# gap-user-service

## Wiremock

Wiremock can be used in local development to stub responses from OneLogin API.

To start wiremock server on http://localhost:8888

```
cd mockOneLogin
docker compose up -d
```

Ensure One Login base URL points to Wiremock

`onelogin.base-url=http://localhost:8888`

Request Mappings are found in `mockOneLogin/wiremock`

New mappings should be added to `mockOneLogin/wiremock/mappings` ensuring file name conforms to using UUID

Responses that return JSON should be added to `mockOneLogin/wiremock/__files`