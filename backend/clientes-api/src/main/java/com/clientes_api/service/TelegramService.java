package com.clientes_api.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envia mensagens de texto via Bot API do Telegram.
 * O destino (chat) deve ser o privado do super administrador — configure {@code TELEGRAM_CHAT_ID}.
 */
@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final String botToken;
    private final String chatId;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;

    private final HttpClient httpClient;

    public TelegramService(
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.chat-id:}") String chatId,
            @Value("${telegram.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${telegram.send-timeout-ms:5000}") int requestTimeoutMs) {
        this.botToken = botToken == null ? "" : botToken.trim();
        this.chatId = chatId == null ? "" : chatId.trim();
        this.connectTimeoutMs = Math.max(500, connectTimeoutMs);
        this.requestTimeoutMs = Math.max(500, requestTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.connectTimeoutMs))
                .build();
    }

    /**
     * @return {@code true} se a API do Telegram respondeu com {@code ok=true}; {@code false} se desconfigurado ou erro de rede/HTTP.
     */
    public boolean sendMessage(String message) {
        if (message == null || message.isBlank()) {
            log.warn("Telegram | mensagem vazia; não enviando");
            return false;
        }
        if (botToken.isEmpty() || chatId.isEmpty()) {
            log.warn("Telegram | TELEGRAM_BOT_TOKEN ou TELEGRAM_CHAT_ID ausente; mensagem não enviada");
            return false;
        }

        String body = "chat_id=" + urlEncode(chatId)
                + "&text=" + urlEncode(truncate(message, 4090))
                + "&disable_web_page_preview=true";

        URI uri = URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage");

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300 && response.body() != null && response.body().contains("\"ok\":true")) {
                return true;
            }
            log.error("Telegram | resposta inesperada | status={} | body={}", code, preview(response.body(), 500));
            return false;
        } catch (Exception e) {
            log.error("Telegram | falha ao enviar mensagem: {}", e.getMessage(), e);
            return false;
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 20) + "\n…(truncado)";
    }

    private static String preview(String body, int max) {
        if (body == null) {
            return "";
        }
        String t = body.trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
