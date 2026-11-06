package br.com.kyoto.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class WhatsAppClient {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final URI endpoint;
    private final String token;

    public WhatsAppClient(String token, String phoneNumberId, String apiVersion) {
        if (!phoneNumberId.matches("[0-9]+") || !apiVersion.matches("v[0-9]+\\.[0-9]+")) {
            throw new IllegalArgumentException("Phone Number ID ou versão da API inválido");
        }
        this.token = token;
        this.endpoint = URI.create("https://graph.facebook.com/" + apiVersion
                + "/" + phoneNumberId + "/messages");
    }

    public void sendText(String recipient, String text) throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of(
                "messaging_product", "whatsapp", "to", recipient, "type", "text",
                "text", Map.of("body", text, "preview_url", false)));
        var request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            // Registra somente os códigos, sem expor tokens ou dados da conversa.
            var error = json.readTree(response.body()).path("error");
            throw new IOException("Falha na Meta: HTTP " + response.statusCode()
                    + ", código " + error.path("code").asText("desconhecido"));
        }
    }
}
