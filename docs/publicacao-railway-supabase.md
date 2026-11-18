# Publicar no Railway com banco no Supabase

O Railway serve as telas e o servidor Java juntos. Não é necessário usar Vercel.
O perfil `railway` mantém as configurações locais e as migrações existentes intactas.

## 1. Criar o banco

1. Entre em https://supabase.com/dashboard e crie um projeto exclusivo chamado `learning-score`.
2. Escolha São Paulo, se disponível, e guarde a senha do banco em um gerenciador de senhas.
3. Abra **Connect**, selecione **Session pooler**, porta **5432**, e veja os parâmetros.
4. Anote host, usuário e nome do banco. Não copie a senha para GitHub, arquivos versionados ou conversa.

Não use Transaction pooler (6543) nesta configuração: o servidor mantém conexões e o Flyway executa migrações.
O aplicativo cria suas tabelas no schema `learning_score`, separado de `public`.
Não adicione esse schema aos schemas expostos pela Data API do Supabase.
O navegador continuará falando com o Java; não precisa de chave anon ou service_role.
Use um projeto novo: esta configuração não copia os dados existentes do computador.

## 2. Preparar o GitHub

Revise e envie as mudanças locais desejadas, incluindo o `Dockerfile`, `.dockerignore`,
`backend/src/main/resources/application-railway.yml` e este guia.
As credenciais configuradas no IntelliJ não são levadas automaticamente para o Railway.

## 3. Criar o serviço Railway

1. Entre em https://railway.com com o GitHub.
2. Crie **New Project → Deploy from GitHub repo** e selecione o repositório Learning Score.
3. Use a raiz do repositório como **Root Directory**; não selecione `backend`.
4. O Railway deve detectar o `Dockerfile` na raiz. Não configure comandos extras de build/start.
5. Em **Variables**, configure os valores abaixo, substituindo os exemplos pelos seus parâmetros.

```text
SPRING_PROFILES_ACTIVE=railway
DB_URL=jdbc:postgresql://HOST_DO_SESSION_POOLER:5432/postgres?sslmode=require
DB_USER=USUARIO_EXATO_DO_SESSION_POOLER
DB_PASSWORD=SENHA_DO_BANCO
PUBLIC_BASE_URL=https://DOMINIO_GERADO_PELO_RAILWAY
```

O usuário do pooler costuma ter o formato `postgres.ID_DO_PROJETO`: copie exatamente o mostrado no Supabase.
A senha fica em `DB_PASSWORD`, separada da URL. `PORT` é fornecida pelo Railway.
O perfil de publicação desativa os dados demo, limita o pool a cinco conexões e exige cookie HTTPS.
`sslmode=require` exige conexão criptografada. Para verificar também a identidade do servidor,
configure o certificado CA do Supabase e `sslmode=verify-full` conforme a documentação oficial.

6. Em **Settings → Networking → Generate Domain**, gere um domínio público.
   Se solicitado, aponte para a porta do serviço (normalmente 8080; respeite `PORT` se definida).
7. Coloque esse endereço HTTPS em `PUBLIC_BASE_URL`, sem barra final, e faça o deploy/redeploy.
   Se a primeira tentativa falhar por falta das variáveis, preencha-as e faça redeploy.
8. Nos logs, confirme o sucesso das migrações Flyway e a inicialização do Spring Boot.

## 4. Envio de e-mails: depende do plano

O Railway bloqueia SMTP nos planos Free, Trial e Hobby. O Gmail SMTP usado localmente
não funcionará nesses planos, mesmo com a senha correta.
Neles, falta integrar um provedor de e-mail por HTTPS (por exemplo, Resend ou outro escolhido).
Também será necessário um remetente autorizado pelo provedor; não presuma que poderá usar
o endereço Gmail como remetente de um serviço externo.

No plano Pro ou superior, o SMTP é permitido. Nesse caso, configure no painel Railway:

```text
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=learningscore25@gmail.com
SPRING_MAIL_PASSWORD=SENHA_DE_APP_CONFIGURADA_SOMENTE_NO_PAINEL
MAIL_FROM=learningscore25@gmail.com
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
```

Não contrate um plano pago sem conferir o preço e escolher conscientemente.
Enquanto o envio não estiver configurado, não considere o fluxo de aprovação por e-mail concluído.

## 5. Conferir o site publicado

- Abrir o domínio HTTPS e cadastrar uma conta nova.
- Entrar, cadastrar disciplina, atividade e nota, sair e entrar de novo para conferir os dados.
- Conferir acesso por celular e navegação nas telas.
- Quando o e-mail estiver habilitado, sugerir conteúdo e receber o convite em uma caixa real.
- Abrir o convite, confirmar a decisão e verificar a justificativa na conta do estudante.

O banco novo começa sem os dados locais. Não foi feita transferência de dados nem publicação automática.
Contas/painéis, domínio, credenciais e escolha do envio de e-mail ainda precisam ser configurados.

Referências:
- https://railway.com/guides/spring-boot
- https://docs.railway.com/networking/outbound-networking
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://supabase.com/docs/guides/platform/ssl-enforcement
