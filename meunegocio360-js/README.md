# MeuNegocio360 — Frontend

Frontend React do MeuNegocio360. O fluxo de autenticação do RF-FE-001 usa a API do Núcleo (`meunegocio360-core`) por `/api/v1`, com proxy local do Vite para `http://localhost:8080`.

## Desenvolvimento

1. Copie `.env.example` para `.env.local`. A URL base deve permanecer relativa: `VITE_API_BASE_URL=/api/v1`.
2. Instale as dependências: `npm install`.
3. Inicie o backend com o perfil `dev` e depois o frontend: `npm run dev`.
4. Acesse `http://localhost:5173/login`.

## Comandos

- `npm run dev`: servidor local Vite, com `/api` encaminhado para `http://localhost:8080`.
- `npm run lint`: ESLint.
- `npm run format`: formatação Prettier.
- `npm run build`: build de produção em `dist/`.
- `npm run preview`: serve o build localmente.

O token de acesso fica somente em memória e o refresh token é mantido pelo cookie `HttpOnly` do backend. Nenhum segredo deve ser colocado em variáveis `VITE_*`.
