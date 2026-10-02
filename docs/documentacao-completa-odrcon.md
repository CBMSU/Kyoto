# Documentação Completa do Sistema - ODRCon

**Gerado em:** 30/03/2026

Documentação técnica para desenvolvedores.

> Versão em PDF: [documentacao-completa-odrcon.pdf](documentacao-completa-odrcon.pdf)

## Sumário

1. [Arquitetura do Sistema](#1-arquitetura-do-sistema)
2. [Modelo de Dados (Banco de Dados)](#2-modelo-de-dados-banco-de-dados)
3. [Edge Functions (Serverless)](#3-edge-functions-serverless)
4. [Segurança](#4-segurança)
5. [Database Functions](#5-database-functions)

---

## 1. Arquitetura do Sistema

O ODRCon é uma aplicação web SPA (Single Page Application) construída com React + TypeScript no frontend e Supabase como BaaS (Backend as a Service).

| Camada | Tecnologias |
|---|---|
| Frontend | React 18 + TypeScript + Vite + Tailwind CSS |
| Backend | Supabase (PostgreSQL + Edge Functions + Auth + Storage) |
| IA/ML | OpenAI GPT via Edge Functions (SophIA) |
| Pagamentos | Mercado Pago API (PIX, Cartão, Boleto) |
| Mensageria | WhatsApp API via WaSender |
| UI Components | shadcn/ui + Radix UI + Framer Motion |
| State Management | TanStack React Query + React Hooks |
| Routing | React Router DOM v6 |

## 2. Modelo de Dados (Banco de Dados)

PostgreSQL gerenciado pelo Supabase com Row-Level Security (RLS) em todas as tabelas.

| Tabela | Descrição |
|---|---|
| `profiles` | Perfis de usuários (user_id, full_name, cpf, phone) |
| `user_roles` | Papéis dos usuários (gerente_plataforma, sindico, condomino, condominio) |
| `condominios` | Dados dos condomínios (nome, endereço, CNPJ, sindico_id) |
| `condomino_residencias` | Associação morador → unidade → condomínio |
| `condomino_condominios` | Associação usuário → condomínio |
| `unidades` | Unidades habitacionais (Casa, Apartamento, Outro) |
| `estrutura` | Estrutura física do condomínio (blocos, torres) |
| `requerimentos` | Requerimentos de mediação (status, partes envolvidas, acordo) |
| `conversas` | Mensagens de mediação (moderação por IA, tipo) |
| `negociacoes` | Negociações financeiras (valor, forma de pagamento) |
| `dividas` | Dívidas condominiais (valor, vencimento, status de pagamento) |
| `documentos` | Estatutos e regimentos internos |
| `anexos_requerimentos` | Arquivos anexados (imagem, vídeo, pdf, áudio) |
| `audit_log` | Log de auditoria de todas as ações |
| `security_events` | Eventos de segurança do sistema |
| `failed_login_attempts` | Tentativas de login falhadas |
| `login_rate_limit` | Rate limiting de login por IP |
| `whatsapp_notifications` | Histórico de notificações WhatsApp |

## 3. Edge Functions (Serverless)

Funções serverless executadas no Supabase Edge Functions (Deno Runtime).

| Função | Descrição |
|---|---|
| `enhanced-login` | Login aprimorado com rate limiting e detecção de ameaças |
| `sophia-chat` | Chatbot IA para mediação de conflitos (OpenAI GPT) |
| `ai-moderator` | Moderação automática de conteúdo por IA |
| `filter-complaint` | Filtro inteligente de reclamações |
| `generate-agreement` | Geração automática de documento de acordo |
| `generate-proposal` | Geração de proposta de acordo via IA |
| `generate-summary` | Resumo automático de conversas |
| `generate-pdf-summary` | Geração de resumo em PDF |
| `transcribe-audio` | Transcrição de áudio para texto (Whisper) |
| `create-morador-user` | Criação de usuário morador |
| `create-sindico-user` | Criação de usuário síndico |
| `delete-user` | Exclusão segura de usuário |
| `confirm-user-email` | Confirmação de e-mail |
| `update-user-password` | Atualização de senha |
| `update-user-email` | Atualização de e-mail |
| `get-user-email` | Consulta segura de e-mail |
| `get-unconfirmed-users` | Listagem de usuários não confirmados |
| `security-check` | Verificação de segurança |
| `create-payment-preference` | Criação de preferência Mercado Pago |
| `mercadopago-webhook` | Webhook de pagamento Mercado Pago |
| `send-whatsapp` | Envio de mensagem WhatsApp |
| `notify-whatsapp` | Notificação automatizada WhatsApp |
| `notify-divida-whatsapp` | Notificação de dívida via WhatsApp |

## 4. Segurança

Mecanismos de segurança implementados no sistema.

| Mecanismo | Descrição |
|---|---|
| Row-Level Security (RLS) | Políticas de acesso em todas as tabelas do banco de dados |
| Rate Limiting | Limite de tentativas de login por IP (5 tentativas / 15 min, bloqueio de 30 min) |
| Audit Log | Registro completo de todas as ações (CRUD) com dados antigos e novos |
| Security Events | Log de eventos de segurança (falhas de login, acessos a dados sensíveis) |
| Password Validation | Validação de força de senha (maiúsculas, minúsculas, números, caracteres especiais) |
| Session Timeout | Expiração automática de sessão após 8 horas |
| SECURITY DEFINER Functions | Funções do banco com execução privilegiada para evitar recursão de RLS |
| Content Moderation | Moderação de conteúdo por IA nas mensagens de mediação |

## 5. Database Functions

Funções PostgreSQL para lógica de negócios no banco de dados.

| Função | Descrição |
|---|---|
| `is_platform_manager()` | Verifica se o usuário é gerente da plataforma |
| `is_sindico_of_condominio()` | Verifica se o usuário é síndico do condomínio |
| `is_condominio_admin()` | Verifica se o usuário é admin do condomínio |
| `can_user_view_condominio()` | Verifica se o usuário pode visualizar o condomínio |
| `get_user_role()` | Retorna o papel do usuário |
| `get_recent_activities()` | Retorna atividades recentes do audit log |
| `get_security_summary()` | Resumo de segurança (falhas de login, eventos) |
| `detect_suspicious_activity()` | Detecção de atividade suspeita |
| `check_rate_limit()` | Verificação de rate limit por IP |
| `validate_password_strength()` | Validação de força de senha |
| `register_audit_log()` | Registro de log de auditoria |
| `registrar_concordancia_requerente()` | Registra concordância do requerente |
| `registrar_concordancia_requerido()` | Registra concordância do requerido |
| `hash_password()` | Hash de senha com bcrypt |
