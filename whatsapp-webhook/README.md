# Webhook WhatsApp — Java

Implementação pequena com Java 17+, servidor HTTP do próprio Java e Jackson para JSON.
Sem interface, banco de dados ou API extra: apenas `GET /webhook` e `POST /webhook`.

## Funcionamento

1. A Meta verifica o endereço por GET; o servidor confere o token e devolve `hub.challenge`.
2. O POST confere `X-Hub-Signature-256` usando o App Secret e o corpo original.
3. Cada mensagem de texto recebida gera a resposta: “Olá! Recebemos sua mensagem. Obrigado pelo contato!”.
4. Status de entrega/leitura e outros tipos de mensagem são confirmados sem resposta.

O envio é uma chamada à Cloud API oficial (`POST /{phone-number-id}/messages`),
pois responder ao HTTP do webhook apenas confirma o recebimento para a Meta;
isso não envia uma mensagem ao contato.

## Executar

Instale JDK 17 ou superior e Maven 3.9+. No terminal, entre nesta pasta e configure:

```sh
export WHATSAPP_VERIFY_TOKEN='um_token_criado_por_voce'
export META_APP_SECRET='app_secret_do_aplicativo'
export WHATSAPP_ACCESS_TOKEN='token_de_acesso'
export WHATSAPP_PHONE_NUMBER_ID='id_numerico_do_numero'
export WHATSAPP_API_VERSION='vXX.0'
export PORT='8080'
mvn compile exec:java
```

Substitua `vXX.0` pela versão suportada exibida no painel da Meta. Todas as variáveis,
exceto `PORT`, são obrigatórias. Phone Number ID é o identificador da Meta, não o telefone.
O `.env.example` é somente um modelo; a aplicação lê variáveis de ambiente, não arquivos `.env`.
Nunca coloque tokens reais no repositório.

## Configurar na Meta

1. No aplicativo Meta com WhatsApp/Cloud API configurado, obtenha Phone Number ID,
   token de acesso com permissão `whatsapp_business_messaging` e App Secret.
2. Publique esta aplicação atrás de HTTPS público, encaminhando para a porta 8080.
   Para desenvolvimento, pode usar um túnel HTTPS até `localhost:8080`.
3. Cadastre a URL de callback `https://seu-dominio/webhook` e o mesmo valor de
   `WHATSAPP_VERIFY_TOKEN` no painel de configuração de webhooks do WhatsApp.
4. Assine o campo `messages` do objeto `whatsapp_business_account` e confira se o
   aplicativo está inscrito na conta WhatsApp Business (WABA).
5. Envie “Oi” de um WhatsApp para o número configurado. Com número de teste da Meta,
   primeiro cadastre/verifique seu telefone como destinatário permitido no painel.

O token temporário do painel expira; para operação contínua configure um token adequado
à sua conta. A resposta deste exemplo ocorre após mensagem do usuário, dentro da janela
de atendimento de 24 horas. Fora dela, o envio exige template aprovado, não implementado aqui.

## Testar a verificação local

Com a aplicação executando e o token do exemplo acima:

```sh
curl 'http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=um_token_criado_por_voce&hub.challenge=123'
```

Resultado esperado: HTTP 200 e corpo `123`. Token incorreto retorna 403.
POST sem assinatura válida retorna 403.

O teste automatizado simula as requisições HTTP e o envio; não abre portas nem precisa de credenciais:

```sh
mvn test-compile exec:java -Dexec.mainClass=br.com.kyoto.whatsapp.WebhookTest -Dexec.classpathScope=test
```

## Onde alterar

- `Application.java`: inicialização e variáveis de ambiente.
- `WebhookHandler.java`: recebe as requisições e devolve as respostas HTTP.
- `WebhookVerification.java`: confere o token de verificação e a assinatura da Meta.
- `MessageProcessor.java`: processa mensagens, evita duplicatas e define o texto da resposta no método `reply`.
- `WhatsAppClient.java`: envio HTTP para a Meta.
- `WebhookTest.java`: cenários locais de verificação, assinatura, mensagens e reenvio.

## Limites deste exemplo

O processamento é síncrono e o envio tem timeout de 10 segundos. Falha no envio retorna
500 para permitir nova entrega pela Meta. A deduplicação guarda até 10 mil IDs por até
24 horas na memória; reiniciar o processo limpa os IDs. Uma interrupção entre o envio
na Meta e o registro local pode causar resposta repetida. Para maior volume ou várias
instâncias, use fila persistente e deduplicação compartilhada antes de confirmar o webhook.

## Referências oficiais

- [Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api/)
- [Webhooks e assinaturas](https://developers.facebook.com/docs/graph-api/webhooks/getting-started/)
- [Mensagens](https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages/)
