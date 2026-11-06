package br.com.kyoto.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Cuida das mensagens e evita responder novamente ao mesmo ID.
final class MessageProcessor {
    private final WebhookHandler.MessageSender sender;
    private final Map<String, Instant> answered = new LinkedHashMap<>();

    MessageProcessor(WebhookHandler.MessageSender sender) {
        this.sender = sender;
    }

    void process(JsonNode event) throws IOException, InterruptedException {
        if ("whatsapp_business_account".equals(event.path("object").asText())) {
            for (JsonNode entry : event.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    if (!"messages".equals(change.path("field").asText())) continue;
                    for (JsonNode message : change.path("value").path("messages")) {
                        reply(message);
                    }
                }
            }
        }
    }

    private synchronized void reply(JsonNode message) throws IOException, InterruptedException {
        if (!"text".equals(message.path("type").asText())) return;
        String id = message.path("id").asText();
        String from = message.path("from").asText();
        String text = message.path("text").path("body").asText();
        if (id.isBlank() || from.isBlank() || text.isBlank()) return;
        Instant now = Instant.now();
        answered.values().removeIf(time -> time.isBefore(now.minusSeconds(86400)));
        if (answered.containsKey(id)) return;
        // Personalize a resposta aqui. O texto recebido está na variável text.
        sender.send(from, "Olá! Recebemos sua mensagem. Obrigado pelo contato!");
        answered.put(id, now);
        if (answered.size() > 10_000) answered.remove(answered.keySet().iterator().next());
        // Se o envio falhar, o ID não é salvo e a Meta pode reenviar o evento.
    }

}
