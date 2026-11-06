package br.com.kyoto.whatsapp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

// Valida o cadastro do webhook e a assinatura dos eventos enviados pela Meta.
final class WebhookVerification {
    private final String verifyToken;
    private final String appSecret;

    WebhookVerification(String verifyToken, String appSecret) {
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
    }

    String challenge(String raw) {
        Map<String, String> query = new HashMap<>();
        if (raw != null) {
            for (String part : raw.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2) {
                    query.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                }
            }
        }
        if ("subscribe".equals(query.get("hub.mode"))
                && verifyToken.equals(query.get("hub.verify_token"))) {
            return query.get("hub.challenge");
        }
        return null;
    }

    boolean validSignature(byte[] body, String signature) throws Exception {
        if (signature == null || !signature.matches("sha256=[a-fA-F0-9]{64}")) return false;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return MessageDigest.isEqual(mac.doFinal(body),
                HexFormat.of().parseHex(signature.substring(7)));
    }

}
