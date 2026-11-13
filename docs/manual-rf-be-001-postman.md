# RF-BE-001 — Roteiro manual no Postman

Este roteiro valida os critérios de aceite de autenticação sem testes automatizados, conforme a fase descrita em RNF-016 e no README.

## Preparação

1. Copie `.env.example` para `.env` na raiz e defina `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `SEED_ADMIN_SENHA` e `SEED_USUARIO_SENHA`. Use senhas de seed com 8 a 72 caracteres. Gere um segredo com `openssl rand -base64 48`; não o compartilhe nem o versione.
2. Para Postman em `http://localhost:8080`, mantenha `SPRING_PROFILES_ACTIVE=dev` e `APP_AUTH_COOKIE_SECURE=false` no ambiente local.
3. Suba o MySQL (`docker compose up -d`) e o Núcleo (`cd meunegocio360-core; .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`). O Flyway cria as tabelas e o perfil `dev` semeia os usuários fictícios.
4. No Postman, crie uma variável `baseUrl` com `http://localhost:8080`.

O usuário ADMIN é `admin@meunegocio360.com.br`; a senha é o valor local de `SEED_ADMIN_SENHA`. O usuário regular é `usuario@meunegocio360.com.br`, com `SEED_USUARIO_SENHA`. Os valores permanecem apenas no `.env` local.

## Fluxo principal

### Login

`POST {{baseUrl}}/api/v1/auth/login`

```json
{ "email": "admin@meunegocio360.com.br", "senha": "<SEED_ADMIN_SENHA>" }
```

Esperado: `200`, JSON com `tokenType`, `accessToken`, `expiresIn` e `usuario`; cabeçalho `Set-Cookie` para `mn360_refresh`, `HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`. O refresh token não aparece no JSON. O Postman deve guardar o cookie para `localhost`; verifique-o no gerenciador de cookies do Postman.

Copie `accessToken` para uma variável `accessToken` (ou extraia por script da coleção) para as chamadas seguintes.

### Identidade autenticada

`GET {{baseUrl}}/api/v1/auth/me`

Authorization → Bearer Token → `{{accessToken}}`.

Esperado: `200` com o mesmo objeto `usuario` do login. Remova o token ou use um token alterado e repita: esperado `401`, `Content-Type: application/problem+json` e `WWW-Authenticate: Bearer`.

### Renovação

`POST {{baseUrl}}/api/v1/auth/refresh`, sem corpo e sem Authorization. O Postman envia o cookie salvo.

Esperado: `200`, novo `accessToken`, objeto `usuario` e novo `Set-Cookie`. O cookie novo deve substituir o anterior. Atualize a variável `accessToken` com o token recém-retornado. Para conferir rotação, reenvie manualmente o valor antigo como cookie `mn360_refresh`: esperado `401`, `INVALID_REFRESH_TOKEN`, cookie limpo e família revogada; o cookie novo também deixa de renovar.

### Logout

Faça login novamente e então envie `POST {{baseUrl}}/api/v1/auth/logout` com o cookie guardado.

Esperado: `204` sem corpo e `Set-Cookie` expirado. Envie logout outra vez sem cookie: continua `204`. Tente renovar após logout: `401 INVALID_REFRESH_TOKEN`.

## Validações e bloqueio

- Login com senha errada e com e-mail inexistente: `401`, `INVALID_CREDENTIALS` e detalhe genérico `E-mail ou senha inválidos.`; nenhum cookie novo.
- Login com e-mail inválido, senha ausente ou senha fora dos limites: `400`, `VALIDATION_ERROR`, `application/problem+json` e lista `errors`.
- Envie cinco senhas incorretas para um usuário de seed: a quinta tentativa retorna `429`, `ACCOUNT_TEMPORARILY_LOCKED` e `Retry-After`. Tentativas seguintes retornam `429`, inclusive com a senha correta, durante o bloqueio. Para repetir sem aguardar, reinicie o banco de desenvolvimento usando o procedimento local apropriado; não faça isso em ambientes compartilhados.
- Em refresh e logout, adicione `Origin: https://origem-invalida.example`: esperado `403`, `ORIGIN_NOT_ALLOWED`. Com `Origin: http://localhost:5173`, a origem está permitida pelo `.env.example`.
- Chame uma rota inexistente sem Authorization: esperado `401`, comprovando o comportamento deny-by-default.

## Conferências úteis

- Em login e refresh, verifique `expiresIn` e a validade do JWT. O payload deve conter `iss=meunegocio360-core`, `sub`, `iat`, `exp`, `jti` e `perfil`.
- No banco local, confira que `refresh_token.token_hash` contém 64 caracteres hexadecimais, sem o valor aleatório do cookie. Não copie tokens reais para logs ou documentação.
- `/actuator/health` deve responder `200` sem detalhes sensíveis. A UI OpenAPI fica em `http://localhost:8080/swagger-ui.html` fora do perfil `prod`.
