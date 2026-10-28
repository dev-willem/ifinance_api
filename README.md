# iFinance API

Backend do iFinance — plataforma de simulação financeira (financiamentos, renda fixa, VPL/TIR/payback).
Java 25 · Spring Boot 4 · PostgreSQL · Arquitetura Hexagonal.

O frontend (Vue 3) vive em um repositório separado: [dev-willem/ifinance](https://github.com/dev-willem/ifinance).

Para rodar localmente (IDE ou Docker dev) veja [DEPLOY.md](./DEPLOY.md), modos 1 e 2.
Para produção, siga o passo a passo abaixo. Ele substitui o modo 3 do DEPLOY.md, que ainda fala de Vercel e DuckDNS.

---

## Produção: Oracle Cloud + Netlify

```
Navegador ──► https://ifinance-app.netlify.app          (frontend + proxy, Netlify)
                 │  /api/*, /oauth2/*, /login/oauth2/*, /logout   (public/_redirects)
                 └─► https://<IP-com-hifens>.sslip.io     (Oracle VM)
                         Nginx :443 (Let's Encrypt)
                           └─► 127.0.0.1:8080  backend-prod  (Docker)
                                                 └─► db-prod  Postgres 16 (Docker, sem porta exposta)
```

O navegador **só fala com o domínio do Netlify**: o Netlify repassa as rotas do backend.
Assim o cookie de sessão é do próprio site. O Firefox e o Safari bloqueiam cookie
de outro domínio, e o login não funcionaria se o frontend chamasse a API direto.

O Postgres roda **na mesma VM**, como o serviço `db-prod` do [compose.yml](./compose.yml).
Os dados ficam no volume Docker `ifinance_postgres_prod_data`.

Situação atual da produção:

| Item | Valor |
|---|---|
| VM | `ifinance-api`, VM.Standard.E2.1.Micro (1 OCPU, 1 GB RAM + 2 GB swap), Ubuntu 24.04, São Paulo |
| IP público | `64.181.167.33` (usuário `ubuntu`) |
| URL da API | `https://64-181-167-33.sslip.io` |
| Frontend | `https://ifinance-app.netlify.app` |
| Pasta na VM | `/opt/ifinance` (`app.jar`, `Dockerfile`, `compose.yml`, `.env`) |

> **Por que sslip.io?** `64-181-167-33.sslip.io` resolve sozinho para `64.181.167.33`,
> sem cadastro. O Let's Encrypt emite certificado para ele, e o Google OAuth exige HTTPS.
> Com domínio próprio, troque o hostname nos passos 4, 5 e 6.

---

### 1. Criar a VM (Oracle Cloud)

1. **Compute → Instances → Create instance**.
2. Imagem: **Canonical Ubuntu 24.04**.
3. Shape:
   - O ideal é a **VM.Standard.A1.Flex** (ARM, Always Free, até 4 OCPU / 24 GB).
   - Se der *"Out of capacity"*, use a **VM.Standard.E2.1.Micro** (AMD, só 1 GB de RAM).
4. Rede: marque **Assign a public IPv4 address**.
5. Cole sua chave SSH pública. Para gerar uma: `ssh-keygen -t ed25519 -f ~/.ssh/ifinance_oci`.
6. Depois de criada, abra as portas 80 e 443:
   - Vá em **Networking → Virtual cloud networks → (sua VCN) → Security Lists → Default**.
   - Adicione **Ingress Rules** TCP **80** e **443** com origem `0.0.0.0/0`. A porta 22 já vem liberada.

```bash
ssh -i ~/.ssh/ifinance_oci ubuntu@<IP>
```

### 2. Preparar a VM (uma vez)

```bash
# Swap de 2 GB (obrigatório na E2.1.Micro: o Java não sobe com 1 GB)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Docker, Nginx e Certbot
sudo apt update
sudo apt install -y docker.io docker-compose-v2 nginx certbot python3-certbot-nginx
sudo usermod -aG docker ubuntu

# A imagem Ubuntu da Oracle bloqueia tudo via iptables, além da Security List
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save

sudo mkdir -p /opt/ifinance && sudo chown ubuntu:ubuntu /opt/ifinance
```

### 3. Subir backend + Postgres

A VM de 1 GB não aguenta compilar com Maven. Por isso o JAR é gerado na sua máquina
e só a imagem de runtime é montada na VM.

**Na sua máquina** (na raiz deste repo):

```bash
./mvnw package -DskipTests
scp -i ~/.ssh/ifinance_oci target/ifinance-*.jar ubuntu@<IP>:/opt/ifinance/app.jar
```

**Na VM**, em `/opt/ifinance`:

```bash
cd /opt/ifinance

# compose.yml oficial (define db-prod + backend-prod)
curl -fsSLo compose.yml https://raw.githubusercontent.com/dev-willem/ifinance_api/main/compose.yml

# Dockerfile só de runtime (o JAR já vem pronto)
cat > Dockerfile <<'EOF'
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S ifinance && adduser -S ifinance -G ifinance
WORKDIR /app
COPY app.jar app.jar
USER ifinance
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=40.0","-XX:+UseSerialGC","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
EOF
```

Crie o `/opt/ifinance/.env`. Os valores do Google vêm do passo 5.

```env
# Postgres (db-prod). A senha é gerada uma vez e nunca mais muda (ela fica gravada no volume)
DB_NAME=ifinance
DB_USERNAME=ifinance
DB_PASSWORD=<saída de: openssl rand -hex 24>

# Google OAuth (passo 5)
GOOGLE_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxx
# O Google devolve para o Netlify, que repassa ao backend (ver arquitetura)
GOOGLE_REDIRECT_URI=https://ifinance-app.netlify.app/login/oauth2/code/google

# Para onde o backend redireciona após o login (sem barra no final). O CORS é só reserva:
# com o proxy do Netlify, o navegador nem faz chamadas cross-origin.
APP_CORS_ALLOWED_ORIGINS=https://ifinance-app.netlify.app
APP_FRONTEND_BASE_URL=https://ifinance-app.netlify.app

# O Nginx chega no container pelo gateway do Docker (172.x), não por 127.0.0.1.
# Sem isso o Spring ignora o X-Forwarded-Proto e gera redirecionamentos http://
TRUSTED_PROXIES='127\.0\.0\.1|::1|172\.(1[6-9]|2[0-9]|3[01])\.\d{1,3}\.\d{1,3}'

BACKEND_IMAGE=ifinance-backend:local
```

```bash
chmod 600 .env
sudo docker build -t ifinance-backend:local .
sudo docker compose --profile prod up -d
sudo docker compose --profile prod ps     # db-prod e backend-prod "healthy" (o Java leva ~1–2 min)
curl -s localhost:8080/actuator/health    # {"status":"UP", ...}
```

> O Flyway cria as tabelas sozinho na primeira subida. O Postgres não publica porta:
> só o backend alcança o banco, pela rede interna do Docker.

### 4. Nginx + HTTPS

`/etc/nginx/sites-available/ifinance`:

```nginx
server {
    listen 80;
    server_name 64-181-167-33.sslip.io;

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
sudo ln -sf /etc/nginx/sites-available/ifinance /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# Certificado + redirecionamento HTTP→HTTPS (a renovação automática já vem configurada)
sudo certbot --nginx -d 64-181-167-33.sslip.io --redirect \
  --agree-tos --register-unsafely-without-email --non-interactive

curl https://64-181-167-33.sslip.io/actuator/health
```

O `X-Forwarded-Proto` é essencial, junto com o `TRUSTED_PROXIES` do `.env`. Sem ele,
o Spring acha que está em HTTP e gera redirecionamentos com `http://`.

### 5. Google OAuth

No [Google Cloud Console](https://console.cloud.google.com):

1. Crie ou selecione um projeto.
2. Configure a tela de consentimento em **APIs & Services → OAuth consent screen**:
   - Tipo **External**.
   - Preencha o nome do app e o e-mail de suporte.
   - Escopos: `openid`, `email` e `profile`.
   - Enquanto o app estiver em **Testing**, só os e-mails cadastrados em **Test users** conseguem logar. Para liberar a todos, clique em **Publish app**.
3. Crie a credencial em **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Tipo: **Web application**.
   - **Authorized JavaScript origins:** `https://ifinance-app.netlify.app`
   - **Authorized redirect URIs:** `https://ifinance-app.netlify.app/login/oauth2/code/google`
     (precisa ser **idêntico** ao `GOOGLE_REDIRECT_URI` do `.env`, no cliente cujo Client ID está no `.env`).
   - Clique em **Save**. A mudança pode levar alguns minutos para valer; enquanto isso, o Google responde `redirect_uri_mismatch`.
4. Copie o **Client ID** e o **Client secret** para o `.env` da VM e recrie o backend:

```bash
cd /opt/ifinance && sudo docker compose --profile prod up -d
```

O fluxo de login:

1. O frontend manda o navegador para `/oauth2/authorization/google`. O Netlify repassa ao backend, que grava o cookie `JSESSIONID` no domínio do Netlify.
2. O Google devolve para `https://ifinance-app.netlify.app/login/oauth2/code/google`, e o Netlify repassa ao backend.
3. O backend conclui a sessão e redireciona para `APP_FRONTEND_BASE_URL/auth/callback`.

### 6. Frontend no Netlify

Tudo depende do arquivo `public/_redirects` do repo do frontend: as regras de proxy do backend
vêm primeiro e o fallback do SPA (`/*  /index.html  200`) vem por último. O `vercel.json` só vale na Vercel.

A variável `VITE_API_BASE_URL` **não deve existir** no Netlify. Sem ela, o frontend usa
caminhos relativos (`/api/...`) e tudo passa pelo proxy. Se a variável for criada ou removida,
rode **Deploys → Trigger deploy → Clear cache and deploy site**: o Vite grava o valor no bundle durante o build.

O service worker do PWA (`vite.config.ts`, `navigateFallbackDenylist`) não intercepta essas rotas.
Se interceptasse, a volta do Google cairia na página 404 do app.

Se o IP ou o domínio da API mudar, atualize as URLs no `_redirects` e faça um novo deploy.

### 7. Checklist de verificação

```bash
curl https://64-181-167-33.sslip.io/actuator/health                    # {"status":"UP"}

# Pelo proxy do Netlify: 302 para accounts.google.com com redirect_uri do Netlify + Set-Cookie JSESSIONID
curl -si https://ifinance-app.netlify.app/oauth2/authorization/google | grep -iE "location|set-cookie"

curl -s -o /dev/null -w '%{http_code}\n' https://ifinance-app.netlify.app/auth/callback   # 200
```

Por fim, abra `https://ifinance-app.netlify.app` numa aba anônima, clique em **Entrar com Google** e confirme que voltou logado.

---

### Atualizar o backend (deploy)

O GitHub Actions de deploy está desativado/quebrado, então o deploy é manual:

```bash
# máquina local
./mvnw package -DskipTests
scp -i ~/.ssh/ifinance_oci target/ifinance-*.jar ubuntu@64.181.167.33:/opt/ifinance/app.jar

# VM
cd /opt/ifinance
sudo docker build -t ifinance-backend:local .
sudo docker compose --profile prod up -d
```

### Operação

```bash
cd /opt/ifinance
sudo docker compose --profile prod logs -f backend-prod     # logs
sudo docker compose --profile prod restart backend-prod     # reiniciar

# Backup do Postgres
sudo docker compose --profile prod exec -T db-prod \
  pg_dump -U ifinance ifinance | gzip > ~/ifinance-$(date +%F).sql.gz

# Restaurar
gunzip -c ~/ifinance-AAAA-MM-DD.sql.gz | sudo docker compose --profile prod exec -T db-prod psql -U ifinance ifinance
```

> **Nunca** rode `docker compose down -v`: o `-v` apaga o volume do Postgres com todos os dados.

### Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `redirect_uri_mismatch` no Google | O URI do Console difere do `GOOGLE_REDIRECT_URI` (http × https, barra no final, hostname), está em outro cliente OAuth ou não foi salvo. Depois de salvar, espere alguns minutos. |
| Login volta para o site, mas o app continua deslogado | O frontend está chamando a API direto, sem passar pelo proxy: há `VITE_API_BASE_URL` no Netlify. Apague a variável e rode *Clear cache and deploy*. |
| Página 404 do app ao voltar do Google | Service worker antigo em cache. Recarregue a página (ou use uma aba anônima). Confira o `navigateFallbackDenylist`. |
| 404 em `/auth/callback` | Falta o fallback `/*  /index.html  200` no `public/_redirects`. |
| Redirecionamentos com `http://` | Falta o `TRUSTED_PROXIES` no `.env` (passo 3). |
| Erros de CSP/`eval` no console na tela do Google | Vêm da página do Google e de extensões do navegador. Podem ser ignorados. |
| `backend-prod` reiniciando ou `unhealthy` | Falta de memória. Confira `free -h` e veja se a swap está ativa. |
| `password authentication failed` | O `DB_PASSWORD` mudou depois da primeira subida. Volte à senha original, ou altere-a dentro do Postgres. |
