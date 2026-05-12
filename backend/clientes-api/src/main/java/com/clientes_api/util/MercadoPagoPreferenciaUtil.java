package com.clientes_api.util;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

/**
 * Montagem de payer/items alinhada às recomendações do Mercado Pago (antifraude / qualidade de integração).
 */
public final class MercadoPagoPreferenciaUtil {

    /**
     * Categoria Mercado Livre Brasil para software comercial / ERP (domínio MLB-COMMERCIAL_SOFTWARES).
     * O valor genérico {@code "others"} pode gerar HTTP 400 na API de preferências em produção (Brasil).
     */
    public static final String ITEM_CATEGORY_PADRAO = "MLB1728";

    private static final Pattern EMAIL_SIMPLES = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private MercadoPagoPreferenciaUtil() {
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
