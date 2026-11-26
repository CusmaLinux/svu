package co.edu.itp.svu.service.report;

import static co.edu.itp.svu.service.report.PqrsStatisticsProjection.*;
import static co.edu.itp.svu.service.report.PqrsStatisticsReceiptException.*;

import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
final class PqrsStatisticsReceipts {

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Base64.Encoder ENCODE = Base64.getUrlEncoder().withoutPadding();
    private static final String AUDIENCE = "svu.pqrs.narrative.docx-pdf";
    private static final Pattern KID = Pattern.compile("[a-zA-Z0-9_-]{1,32}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String active;
    private final Map<String, byte[]> keys;
    private final int minutes;

    PqrsStatisticsReceipts(Environment env) {
        String prefix = "svu.pqrs-statistics.receipt.";
        String activeKid = "";
        Map<String, byte[]> ring = new HashMap<>();
        int ttl = 0;
        try {
            activeKid = env.getProperty(prefix + "active-kid", "");
            ttl = Integer.parseInt(env.getProperty(prefix + "ttl-minutes", "30"));
            String jwtKey = env.getProperty("jhipster.security.authentication.jwt.base64-secret", "");
            byte[] signingKey = Base64.getDecoder().decode(jwtKey);
            for (String entry : env.getProperty(prefix + "keys", "").split(",", -1)) {
                String[] pair = entry.split(":", -1);
                if (pair.length != 2 || !KID.matcher(pair[0]).matches()) throw new IllegalArgumentException();
                byte[] key = Base64.getDecoder().decode(pair[1]);
                if (
                    key.length < 32 ||
                    MessageDigest.isEqual(signingKey, key) ||
                    ring.values().stream().anyMatch(k -> MessageDigest.isEqual(k, key)) ||
                    ring.putIfAbsent(pair[0], key) != null
                ) throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException invalidConfiguration) {
            ring.clear();
        }
        active = activeKid;
        keys = Map.copyOf(ring);
        minutes = ttl;
    }

    void requireConfigured() {
        if (!keys.containsKey(active) || minutes < 1 || minutes > 60) throw unavailable();
    }

    record Issued(String receipt, Instant expiresAt) {
        @Override
        public String toString() {
            return "Issued preview receipt";
        }
    }

    record Verified(String kid, String nonce, String contentTag, Instant observation, Instant expiry) {
        @Override
        public String toString() {
            return "Authenticated preview reference";
        }
    }

    Issued issue(Jwt jwt, PqrsStatisticsFilter filter, Instant observation, Instant now, byte[] content) {
        requireConfigured();
        Instant expiry = now.plus(Duration.ofMinutes(minutes));
        if (jwt.getExpiresAt().isBefore(expiry)) expiry = jwt.getExpiresAt();
        if (!now.isBefore(expiry)) throw new PqrsStatisticsReceiptException(401, "unauthenticated");
        if (now.isBefore(observation)) throw unavailable();
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String nonce = ENCODE.encodeToString(salt);
        byte[] key = keys.get(active);
        var header = JSON.createObjectNode().put("v", 1).put("kid", active);
        var payload = JSON.createObjectNode()
            .put("aud", AUDIENCE)
            .put("nonce", nonce)
            .put("iat", now.toEpochMilli())
            .put("exp", expiry.toEpochMilli())
            .put("obs", observation.toEpochMilli())
            .put("model", MODEL)
            .put("method", METHODOLOGY)
            .put("catalog", CATALOG)
            .put("holder", holder(key, nonce, jwt))
            .put("filter", tag(key, "filter", nonce, bytes(filter(filter))))
            .put("content", tag(key, "content", nonce, content));
        String body = segment(header) + "." + segment(payload);
        return new Issued(body + "." + signature(key, body), Instant.ofEpochMilli(expiry.toEpochMilli()));
    }

    Verified authenticate(String receipt, Jwt jwt, PqrsStatisticsFilter filter, Instant now) {
        requireConfigured();
        if (receipt == null || receipt.length() > 4096 || !receipt.chars().allMatch(c -> c >= 0x21 && c <= 0x7e)) throw missing();
        String[] parts = receipt.split("\\.", -1);
        if (parts.length != 3) throw missing();
        var header = object(parts[0]);
        String kid = header.path("kid").textValue();
        if (kid == null || !KID.matcher(kid).matches()) throw missing();
        byte[] key = keys.get(kid);
        if (key == null || !equal(decode(parts[2]), decode(signature(key, parts[0] + "." + parts[1])))) throw missing();
        var p = object(parts[1]);
        String nonce = p.path("nonce").asText("");
        if (
            decode(nonce).length != 16 || !matches(p, "holder", holder(key, nonce, jwt)) || !AUDIENCE.equals(p.path("aud").textValue())
        ) throw missing();
        if (
            header.size() != 2 ||
            !version(header, "v", 1) ||
            !version(p, "model", MODEL) ||
            !version(p, "method", METHODOLOGY) ||
            !version(p, "catalog", CATALOG)
        ) throw new PqrsStatisticsReceiptException(409, "previewVersionChanged");
        if (p.size() != 11) throw missing();
        Instant issued = time(p, "iat"), expiry = time(p, "exp"), observed = time(p, "obs");
        if (
            issued.isAfter(now) || observed.isAfter(issued) || !expiry.isAfter(issued) || expiry.isAfter(jwt.getExpiresAt())
        ) throw missing();
        if (!now.isBefore(expiry)) throw new PqrsStatisticsReceiptException(410, "previewExpired");
        if (!matches(p, "filter", tag(key, "filter", nonce, bytes(filter(filter))))) throw new PqrsStatisticsReceiptException(
            409,
            "previewFilterMismatch"
        );
        String contentTag = p.path("content").asText("");
        if (decode(contentTag).length != 32) throw missing();
        return new Verified(kid, nonce, contentTag, observed, expiry);
    }

    void compare(Verified reference, byte[] content) {
        if (!equal(decode(reference.contentTag()), decode(tag(keys.get(reference.kid()), "content", reference.nonce(), content)))) {
            throw new PqrsStatisticsReceiptException(409, "statisticsChanged");
        }
    }

    private static boolean version(JsonNode n, String key, int expected) {
        return n.path(key).isInt() && n.get(key).intValue() == expected;
    }

    private static Instant time(JsonNode n, String key) {
        var value = n.path(key);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) throw missing();
        return Instant.ofEpochMilli(value.longValue());
    }

    private static boolean matches(JsonNode p, String field, String expected) {
        return equal(decode(p.path(field).asText("")), decode(expected));
    }

    private static boolean equal(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    private static String holder(byte[] key, String nonce, Jwt jwt) {
        return tag(key, "holder", nonce, bytes(List.of(jwt.getSubject(), jwt.getTokenValue())));
    }

    private static String tag(byte[] key, String domain, String nonce, byte[] value) {
        return mac(key, bytes(List.of("svu.pqrs.receipt/1", domain, nonce, ENCODE.encodeToString(value))));
    }

    private static String signature(byte[] key, String body) {
        return mac(key, bytes(List.of("svu.pqrs.receipt/1", "signature", body)));
    }

    private static String mac(byte[] key, byte[] value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return ENCODE.encodeToString(mac.doFinal(value));
        } catch (GeneralSecurityException e) {
            throw unavailable();
        }
    }

    private static String segment(ObjectNode value) {
        return ENCODE.encodeToString(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] decode(String part) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(part);
            if (part.isEmpty() || !ENCODE.encodeToString(bytes).equals(part)) throw missing();
            return bytes;
        } catch (IllegalArgumentException e) {
            throw missing();
        }
    }

    private static JsonNode object(String part) {
        try {
            var node = JSON.readTree(decode(part));
            if (node == null || !node.isObject()) throw missing();
            return node;
        } catch (IOException e) {
            throw missing();
        }
    }
}
