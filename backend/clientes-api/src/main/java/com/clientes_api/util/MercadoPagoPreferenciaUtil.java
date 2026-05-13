package com.clientes_api.util;

import com.clientes_api.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

/**
 * Montagem de payer/items alinhada às recomendações do Mercado Pago (antifraude / qualidade de integração).
 */
public final class MercadoPagoPreferenciaUtil {

    /**
     * Categoria no formato aceito pela API de preferências (Checkout Pro), alinhada a payload mínimo validado.
     * Evita {@code "others"} e IDs MLB longos que podem variar por ambiente.
     */
    public static final String ITEM_CATEGORY_PADRAO = "services";

    private static final Pattern EMAIL_SIMPLES = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private MercadoPagoPreferenciaUtil() {
    }

    /**
     * Campos mínimos exigidos/recomendados pelo Mercado Pago para conciliação, webhooks e índice de aprovação.
     */
    public static void assertPreferenciaConformidade(ObjectNode root) {
        if (textTrimOrEmpty(root.path("external_reference")).isEmpty()) {
            throw new BusinessException(
                    "external_reference é obrigatório: use um código único para correlacionar payment_id com o registro interno.");
        }
        if (textTrimOrEmpty(root.path("notification_url")).isEmpty()) {
            throw new BusinessException(
                    "notification_url é obrigatório: configure mercadopago.notification-url com o endpoint HTTPS do webhook.");
        }
        JsonNode items = root.path("items");
        if (!items.isArray() || items.isEmpty()) {
            throw new BusinessException("A preferência deve conter ao menos um item em items[].");
        }
        JsonNode item0 = items.get(0);
        if (textTrimOrEmpty(item0.path("id")).isEmpty()) {
            throw new BusinessException("items[0].id é obrigatório (código do item).");
        }
        if (textTrimOrEmpty(item0.path("title")).isEmpty()) {
            throw new BusinessException("items[0].title é obrigatório (nome do item).");
        }
        if (textTrimOrEmpty(item0.path("description")).isEmpty()) {
            throw new BusinessException("items[0].description é obrigatório (descrição do item).");
        }
        if (textTrimOrEmpty(item0.path("category_id")).isEmpty()) {
            throw new BusinessException("items[0].category_id é obrigatório (categoria do item).");
        }
    }

    private static String textTrimOrEmpty(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText("").trim();
        }
        return n.asText("").trim();
    }

    public static boolean emailValidoParaCheckout(String email) {
        if (email == null) {
            return false;
        }
        String e = email.trim();
        if (e.length() < 5 || !EMAIL_SIMPLES.matcher(e).matches()) {
            return false;
        }
        String lower = e.toLowerCase();
        if (lower.endsWith("@placeholder.local") || lower.endsWith(".invalid")) {
            return false;
        }
        return true;
    }

    /**
     * Primeiro e-mail válido entre candidatos, ou {@code null}.
     */
    public static String primeiroEmailValido(String... candidatos) {
        if (candidatos == null) {
            return null;
        }
        for (String c : candidatos) {
            if (emailValidoParaCheckout(c)) {
                return c.trim();
            }
        }
        return null;
    }

    /**
     * Preenche {@code first_name}, {@code last_name} e {@code name} no nó {@code payer}.
     */
    public static void preencherPayerNome(ObjectNode payer, String nomeCompleto) {
        String nome = nomeCompleto == null ? "" : nomeCompleto.trim();
        if (nome.isEmpty()) {
            payer.put("first_name", "Cliente");
            payer.put("last_name", "ERP");
            payer.put("name", "Cliente ERP");
            return;
        }
        int sp = nome.indexOf(' ');
        if (sp < 0) {
            payer.put("first_name", nome);
            payer.put("last_name", nome);
        } else {
            String first = nome.substring(0, sp).trim();
            String last = nome.substring(sp + 1).trim();
            if (last.isEmpty()) {
                last = first;
            }
            payer.put("first_name", first);
            payer.put("last_name", last);
        }
        payer.put("name", nome);
    }

    /**
     * CPF (11 dígitos) ou CNPJ (14) no formato esperado pelo MP, se {@code documento} tiver só dígitos suficientes.
     */
    public static void preencherPayerIdentificacaoBrasil(ObjectNode payer, String documento) {
        if (documento == null || documento.isBlank()) {
            return;
        }
        String digits = documento.replaceAll("\\D", "");
        if (digits.chars().distinct().count() <= 1) {
            return;
        }
        if (digits.length() == 14) {
            ObjectNode id = payer.putObject("identification");
            id.put("type", "CNPJ");
            id.put("number", digits);
        } else if (digits.length() == 11) {
            ObjectNode id = payer.putObject("identification");
            id.put("type", "CPF");
            id.put("number", digits);
        }
    }

    /**
     * Telefone BR simples {@code (DD) número} ou só dígitos.
     */
    public static void preencherPayerTelefoneBrasil(ObjectNode payer, String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return;
        }
        String digits = telefone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            return;
        }
        String ddd;
        String numero;
        if (digits.length() == 11) {
            ddd = digits.substring(0, 2);
            numero = digits.substring(2);
        } else {
            ddd = digits.substring(0, 2);
            numero = digits.substring(2);
        }
        ObjectNode phone = payer.putObject("phone");
        phone.put("area_code", ddd);
        phone.put("number", numero);
    }
}
