package br.com.kyoto.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WebhookHandler implements HttpHandler {
    @FunctionalInterface
    public interface MessageSender {
        void send(String recipient, String text) throws IOException, InterruptedException;
    }

    private final ObjectMapper json = new ObjectMapper();
    private final WebhookVerification verification;
    private final MessageProcessor messages;

    public WebhookHandler(String verifyToken, String appSecret, MessageSender sender) {
        this.verification = new WebhookVerification(verifyToken, appSecret);
        this.messages = new MessageProcessor(sender);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"/webhook".equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "Não encontrado");
                return;
            }
            try {
                switch (exchange.getRequestMethod()) {
                    case "GET" -> verify(exchange);
                    case "POST" -> receive(exchange);
                    default -> {
                        exchange.getResponseHeaders().set("Allow", "GET, POST");
                        respond(exchange, 405, "Método não permitido");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                respond(exchange, 503, "Tente novamente");
            } catch (Exception e) {
                System.err.println("Falha ao processar webhook: " + e.getClass().getSimpleName());
                respond(exchange, 500, "Falha ao processar evento");
            }
        }
    }

    private void verify(HttpExchange exchange) throws IOException {
        try {
            String challenge = verification.challenge(exchange.getRequestURI().getRawQuery());
            if (challenge == null) {
                respond(exchange, 403, "Verificação recusada");
            } else {
                respond(exchange, 200, challenge);
            }
        } catch (IllegalArgumentException e) {
            respond(exchange, 400, "Consulta inválida");
        }
    }

    private void receive(HttpExchange exchange) throws Exception {
        byte[] body = exchange.getRequestBody().readNBytes(1_048_577);
        if (body.length > 1_048_576) {
            respond(exchange, 413, "Evento muito grande");
            return;
        }
        if (!verification.validSignature(body, exchange.getRequestHeaders().getFirst("X-Hub-Signature-256"))) {
            respond(exchange, 403, "Assinatura inválida");
            return;
        }
        JsonNode event;
        try {
            event = json.readTree(body);
        } catch (JsonProcessingException e) {
            respond(exchange, 400, "JSON inválido");
            return;
        }
        if (event == null || !event.isObject()) {
            respond(exchange, 400, "Evento inválido");
            return;
        }
        messages.process(event);
        // Eventos de status e mensagens não textuais também precisam ser confirmados.
        respond(exchange, 200, "EVENT_RECEIVED");
    }

    private static void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
