package br.com.kyoto.whatsapp;

import com.sun.net.httpserver.*;
import java.io.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

// Teste executável, sem JUnit e sem chamadas reais à Meta.
public class WebhookTest {
    public static void main(String[] args) throws Exception {
        var sent = new AtomicInteger();
        var fail = new java.util.concurrent.atomic.AtomicBoolean();
        var handler = new WebhookHandler("verify", "secret", (to, text) -> {
            check(to.equals("5582999999999"), "destinatário");
            check(text.contains("Recebemos"), "resposta");
            if (fail.get()) throw new java.io.IOException("Falha simulada");
            sent.incrementAndGet();
        });
        var verified = new Exchange("GET", "/webhook?hub.mode=subscribe&hub.verify_token=verify&hub.challenge=123", "");
        handler.handle(verified);
        check(verified.code == 200 && verified.output.toString(StandardCharsets.UTF_8).equals("123"), "challenge");
        var rejected = new Exchange("GET", "/webhook", "");
        handler.handle(rejected);
        check(rejected.code == 403, "token inválido");
        var wrongPath = new Exchange("GET", "/webhook/outro", "");
        handler.handle(wrongPath);
        check(wrongPath.code == 404, "caminho inválido");
        var wrongMethod = new Exchange("PUT", "/webhook", "");
        handler.handle(wrongMethod);
        check(wrongMethod.code == 405 && "GET, POST".equals(wrongMethod.response.getFirst("Allow")),
                "método não permitido");
        var wrongMode = new Exchange("GET", "/webhook?hub.mode=invalid&hub.verify_token=verify&hub.challenge=123", "");
        handler.handle(wrongMode);
        check(wrongMode.code == 403, "modo inválido");
        var malformedQuery = new Exchange("GET", "/webhook?hub.verify_token=%FF", "");
        handler.handle(malformedQuery);
        check(malformedQuery.code == 403, "token malformado");
        new WhatsAppClient("test-token", "123456", "v99.0");
        try {
            new WhatsAppClient("test-token", "123456", "invalid");
            throw new AssertionError("versão inválida aceita");
        } catch (IllegalArgumentException expected) {
            // Configuração inválida deve falhar antes de qualquer chamada HTTP.
        }
        String body = """
                    {"object":"whatsapp_business_account","entry":[{"changes":[{
                    "field":"messages","value":{"messages":[{"id":"wamid.1",
                    "from":"5582999999999","type":"text","text":{"body":"Oi"}}]}}]}]}
                    """;
        check(post(handler, body, "sha256=invalid") == 403, "assinatura inválida");
        check(post(handler, body, null) == 403, "assinatura ausente");
        check(post(handler, body.replace("Oi", "Alterado"), sign(body)) == 403, "corpo adulterado");
        check(post(handler, body, sign(body)) == 200, "recebimento");
        check(post(handler, body, sign(body)) == 200 && sent.get() == 1, "duplicata");
        String status = body.replace("\"messages\":[", "\"statuses\":[");
        check(post(handler, status, sign(status)) == 200 && sent.get() == 1, "status");
        String image = body.replace("\"type\":\"text\"", "\"type\":\"image\"");
        check(post(handler, image, sign(image)) == 200 && sent.get() == 1, "mídia");
        check(post(handler, "{", sign("{")) == 400, "JSON inválido");
        check(post(handler, "", sign("")) == 400, "corpo vazio");
        check(post(handler, "[]", sign("[]")) == 400, "evento inválido");
        String oversized = "x".repeat(1_048_577);
        check(post(handler, oversized, sign(oversized)) == 413, "limite do corpo");
        String missingId = body.replace("\"id\":\"wamid.1\",", "");
        check(post(handler, missingId, sign(missingId)) == 200 && sent.get() == 1, "mensagem incompleta");
        String batch = body.replace("wamid.1", "wamid.batch1").replace(
                "\"body\":\"Oi\"}}]", "\"body\":\"Oi\"}},{\"id\":\"wamid.batch2\","
                + "\"from\":\"5582999999999\",\"type\":\"text\",\"text\":{\"body\":\"Olá\"}}]");
        check(post(handler, batch, sign(batch)) == 200 && sent.get() == 3, "múltiplas mensagens");
        check(post(handler, batch, sign(batch)) == 200 && sent.get() == 3, "duplicatas em lote");
        String next = body.replace("wamid.1", "wamid.2");
        fail.set(true);
        check(post(handler, next, sign(next)) == 500, "falha de envio");
        fail.set(false);
        check(post(handler, next, sign(next)) == 200 && sent.get() == 4, "reenvio após falha");
        System.out.println("OK: todos os testes de verificação, segurança, mensagens, lotes e falha/reenvio passaram.");
    }

    private static int post(WebhookHandler handler, String body, String signature) throws Exception {
        var exchange = new Exchange("POST", "/webhook", body);
        if (signature != null) exchange.request.set("X-Hub-Signature-256", signature);
        handler.handle(exchange);
        return exchange.code;
    }

    // Substitui apenas o transporte HTTP; executa o handler real, sem abrir portas.
    private static class Exchange extends HttpExchange {
        final Headers request = new Headers();
        final Headers response = new Headers();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final String method;
        final URI uri;
        final InputStream input;
        int code;
        Exchange(String method, String path, String body) {
            this.method = method;
            this.uri = URI.create(path);
            this.input = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
        public Headers getRequestHeaders() { return request; }
        public Headers getResponseHeaders() { return response; }
        public URI getRequestURI() { return uri; }
        public String getRequestMethod() { return method; }
        public HttpContext getHttpContext() { return null; }
        public void close() { }
        public InputStream getRequestBody() { return input; }
        public OutputStream getResponseBody() { return output; }
        public void sendResponseHeaders(int status, long length) { code = status; }
        public InetSocketAddress getRemoteAddress() { return null; }
        public int getResponseCode() { return code; }
        public InetSocketAddress getLocalAddress() { return null; }
        public String getProtocol() { return "HTTP/1.1"; }
        public Object getAttribute(String name) { return null; }
        public void setAttribute(String name, Object value) { }
        public void setStreams(InputStream in, OutputStream out) { }
        public HttpPrincipal getPrincipal() { return null; }
    }

    private static String sign(String body) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static void check(boolean result, String name) {
        if (!result) throw new AssertionError(name);
    }
}
