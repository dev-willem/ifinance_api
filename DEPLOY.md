# iFinance API — Guia de Deploy

> Este repositório contém **apenas o backend**. O frontend (Vue) vive em
> [dev-willem/ifinance](https://github.com/dev-willem/ifinance) e é publicado separadamente na Vercel.
> Três formas de rodar este backend: local (IDE), Docker dev e produção (VPS + Vercel).

---

## Pré-requisitos

| Ferramenta | Versão mínima | Instalação |
|---|---|---|
| Java | 25 | [Eclipse Temurin](https://adoptium.net) |
| Maven | via `mvnw` | incluído no repo |
| Docker Desktop | 27+ | [docker.com](https://www.docker.com/products/docker-desktop) |
| Docker Compose | v2 (plugin) | incluído no Docker Desktop |
| PostgreSQL | 16 (ou via Docker) | apenas para modo local sem Docker |

---

## Modo 1 — Local (IDE / linha de comando)

**Quando usar:** desenvolvimento ativo, hot-reload do Spring DevTools, debug com breakpoints.

### 1.1 — Banco de dados

```bash
# Se não tiver PostgreSQL local, suba apenas o banco via Docker:
docker compose up db -d
```

Ou use um PostgreSQL já instalado:
```sql
CREATE DATABASE ifinance_dev;
CREATE USER ifinance WITH PASSWORD 'ifinance';
GRANT ALL PRIVILEGES ON DATABASE ifinance_dev TO ifinance;
```

### 1.2 — Variáveis de ambiente

```bash
cp .env.example .env
# Edite .env com suas credenciais Google OAuth2
```

Variáveis necessárias para o perfil local:
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ifinance_dev
DB_USERNAME=postgres   # ou ifinance
DB_PASSWORD=1234       # ou a senha que criou
GOOGLE_CLIENT_ID=seu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=seu-client-secret
SERVER_PORT=8888       # opcional, padrão é 8888
```

### 1.3 — Rodar

```bash
./mvnw spring-boot:run

# Ou com variáveis explícitas:
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

Serviço disponível em: `http://localhost:8888`
Swagger UI: `http://localhost:8888/swagger-ui.html`

Para testar com o frontend rodando localmente (repo separado), aponte o Vite dele
(`VITE_DEV_BACKEND_URL`) para `http://localhost:8888`.

---

## Modo 2 — Docker dev (backend + banco containerizados)

**Quando usar:** reproduzir bugs de ambiente sem IDE, sem precisar instalar Java/Maven/Postgres.

```bash
cp .env.example .env
# Preencha pelo menos: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET

docker compose --profile docker up --build
```

| Serviço | URL |
|---|---|
| Backend | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Banco | localhost:5432 |

```bash
# Parar
docker compose --profile docker down
# Reset completo do banco
docker compose --profile docker down -v

# Logs
docker compose --profile docker logs -f backend
```

---

## Modo 3 — Produção (VPS + frontend na Vercel)

**Quando usar:** deploy real. Este backend + PostgreSQL rodam em containers Docker numa VPS
(ex.: Oracle Cloud Free Tier), atrás de Nginx com HTTPS via Let's Encrypt. O frontend
(repo [dev-willem/ifinance](https://github.com/dev-willem/ifinance)) é publicado à parte na Vercel.

### 3.0 — Criar a VPS (Oracle Cloud Free Tier)

1. Console Oracle Cloud → **Compute → Instances → Create Instance**
2. Imagem: **Canonical Ubuntu** (22.04 ou 24.04)
3. Shape: **VM.Standard.A1.Flex** (ARM, Always Free) — recomendado **2 OCPU / 6–12 GB RAM**.
   Se der *"Out of host capacity"*, tente outro *Availability Domain* ou use
   **VM.Standard.E2.1.Micro** (AMD, sempre disponível, mas só 1 GB RAM).
4. Marque **"Assign a public IPv4 address"** e gere/cole um par de chaves SSH
5. Em **Networking → Security Lists**, libere as portas **80** e **443** para `0.0.0.0/0`
   (a 22 já vem liberada)

### 3.1 — Domínio gratuito (DuckDNS)

Sem domínio próprio, HTTPS válido (exigido para OAuth2 e para o cookie de sessão
cross-domain — ver nota em 3.4) precisa de um hostname público:

1. https://www.duckdns.org → login → criar subdomínio, ex.: `ifinance-api` → `ifinance-api.duckdns.org`
2. Cole o IP público da instância e clique **update ip**

### 3.2 — Preparar o servidor (uma vez)

```bash
# 1. Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 2. Diretório do projeto
sudo mkdir -p /opt/ifinance
sudo chown $USER:$USER /opt/ifinance

# 3. .env de produção (ver 3.3)
nano /opt/ifinance/.env

# 4. Chave pública SSH do GitHub Actions
echo "ssh-ed25519 AAAA... github-actions" >> ~/.ssh/authorized_keys

# 5. Nginx + Certbot
sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
```

`/etc/nginx/sites-available/ifinance`:
```nginx
server {
    listen 80;
    server_name ifinance-api.duckdns.org;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/ifinance /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d ifinance-api.duckdns.org
```

### 3.3 — Configurar o repositório GitHub

**Secrets** (Settings → Secrets and variables → Actions):

| Secret | Descrição |
|---|---|
| `DEPLOY_SSH_KEY` | Chave SSH privada para acessar o servidor |
| `DEPLOY_HOST` | IP ou hostname do servidor (ex: `ifinance-api.duckdns.org`) |
| `DEPLOY_USER` | Usuário SSH (ex: `ubuntu`) |
| `DEPLOY_PATH` | Caminho no servidor (ex: `/opt/ifinance`) |

**Variables:**

| Variable | Descrição |
|---|---|
| `PRODUCTION_URL` | URL do backend para health-check pós-deploy |

**`.env` do servidor (`/opt/ifinance/.env`):**
```env
DB_NAME=ifinance
DB_USERNAME=ifinance
DB_PASSWORD=senha-forte-aqui
GOOGLE_CLIENT_ID=seu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=seu-client-secret
GOOGLE_REDIRECT_URI=https://ifinance-api.duckdns.org/login/oauth2/code/google
APP_CORS_ALLOWED_ORIGINS=https://seu-projeto.vercel.app
APP_FRONTEND_BASE_URL=https://seu-projeto.vercel.app
SERVER_PORT=8080
BACKEND_IMAGE=ghcr.io/dev-willem/ifinance-backend:latest
```

> A porta 8080 é publicada apenas em `127.0.0.1` (ver `compose.yml`) — o Nginx configurado
> em 3.2 repassa `https://ifinance-api.duckdns.org` → `http://127.0.0.1:8080`.

### 3.4 — Fluxo CI/CD

```
git push origin main
        │
        ├─► CI Backend (.github/workflows/ci-backend.yml)
        │     compile → test → build JAR → docker build → push GHCR
        │
        └─► Deploy (.github/workflows/deploy.yml)
              SSH → docker compose pull → docker compose up -d
```

> **Cookie cross-domain:** frontend (Vercel) e backend (DuckDNS) ficam em domínios
> diferentes, então o cookie `JSESSIONID` precisa de `SameSite=None; Secure`
> (já configurado em `application-prod.yml`) — só funciona com HTTPS válido dos dois lados.

### 3.5 — Deploy manual

```bash
# Actions → Deploy — Produção → Run workflow
# Ou direto no servidor:
cd /opt/ifinance
docker compose --profile prod pull
docker compose --profile prod up -d --remove-orphans
```

### 3.6 — Verificar saúde

```bash
curl https://ifinance-api.duckdns.org/actuator/health
# Esperado: {"status":"UP"}
```

---

## Variáveis de ambiente — referência completa

| Variável | Perfil | Padrão | Obrigatória |
|---|---|---|---|
| `DB_HOST` | prod | — | sim |
| `DB_PORT` | prod | `5432` | não |
| `DB_NAME` | prod | — | sim |
| `DB_USERNAME` | docker/prod | `ifinance` | sim em prod |
| `DB_PASSWORD` | docker/prod | `ifinance` | sim em prod |
| `GOOGLE_CLIENT_ID` | todos | — | sim |
| `GOOGLE_CLIENT_SECRET` | todos | — | sim |
| `GOOGLE_REDIRECT_URI` | prod | — | **sim** (sem fallback; deploy falha se ausente) |
| `APP_CORS_ALLOWED_ORIGINS` | prod | — | **sim** (URL do frontend na Vercel) |
| `APP_FRONTEND_BASE_URL` | prod | — | **sim** (URL do frontend — redirect pós-login) |
| `TRUSTED_PROXIES` | prod | `127\.0\.0\.1\|::1` | não |
| `SERVER_PORT` | docker/prod | `8080` | não |
| `BACKEND_IMAGE` | prod compose | `ghcr.io/.../ifinance-backend:latest` | não |

---

## Configuração de OAuth2 Google

1. [Google Cloud Console](https://console.cloud.google.com) → **APIs & Services → Credentials → OAuth 2.0 Client ID** → **Web application**
2. Origens autorizadas:
   - Local: `http://localhost:8888`, `http://localhost:7000` (frontend local)
   - Docker dev: `http://localhost:8080`, `http://localhost:3000`
   - Prod: `https://ifinance-api.duckdns.org` (backend) e a URL da Vercel (frontend)
3. URIs de redirecionamento:
   - Local: `http://localhost:8888/login/oauth2/code/google`
   - Docker dev: `http://localhost:8080/login/oauth2/code/google`
   - Prod: `https://ifinance-api.duckdns.org/login/oauth2/code/google`
     — precisa ser **idêntico** ao `GOOGLE_REDIRECT_URI` do servidor

---

## Perfis Spring Boot

| Perfil | Ativação | Banco | SQL | Swagger |
|---|---|---|---|---|
| `local` | padrão (IDE) | `localhost:5432` | debug | habilitado |
| `docker` | compose docker | `db:5432` | off | habilitado |
| `prod` | compose prod | env vars | off | desabilitado |
| `test` | testes automáticos | Testcontainers | off | n/a |

---

## Troubleshooting

**Backend não inicia — `InvestmentRequestMapper` não encontrado**
→ Falha do MapStruct (annotation processor). Rode `./mvnw compile` primeiro.

**`GOOGLE_CLIENT_ID` não definido**
→ Verifique se o arquivo `.env` existe na raiz e foi carregado pelo Spring.

**Frontend mostra erros de rede / CORS**
→ Confirme que `APP_CORS_ALLOWED_ORIGINS` no servidor bate exatamente com a URL da Vercel
  (sem barra final) e que o frontend usa `VITE_API_BASE_URL` apontando pra este backend.

**Testcontainers falha no CI**
→ O runner do GitHub Actions tem Docker disponível. Self-hosted runner precisa instalar Docker.

**Container backend nunca fica healthy**
→ `docker compose logs backend`. Health check usa `/actuator/health`, `start_period` de 60s.

**Login Google completa mas volta como não autenticado**
→ Normalmente é o cookie de sessão não sendo enviado cross-domain. Confirme HTTPS válido
  em ambos os domínios e `same-site: none` em `application-prod.yml` (já configurado).
