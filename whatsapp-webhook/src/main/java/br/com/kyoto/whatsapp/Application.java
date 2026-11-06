package br.com.kyoto.whatsapp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Application {
    public static void main(String[] args) throws Exception {
        var client = new WhatsAppClient(required("WHATSAPP_ACCESS_TOKEN"),
                required("WHATSAPP_PHONE_NUMBER_ID"), required("WHATSAPP_API_VERSION"));
        var webhook = new WebhookHandler(required("WHATSAPP_VERIFY_TOKEN"),
                required("META_APP_SECRET"), client::sendText);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/webhook", webhook);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
        server.start();
        System.out.println("Webhook disponível na porta " + port + ", caminho /webhook");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Configure a variável " + name);
        }
        return value;
    }
}
