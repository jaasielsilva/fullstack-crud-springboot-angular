package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.EmailConfig;
import com.clientes_api.repository.EmailConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private EmailConfigRepository configRepository;

    public void enviarEmailRecuperacao(String para, String token, Long tenantId) {
        EmailConfig config = buscarConfigObrigatoria(tenantId);

        JavaMailSender mailSender = createMailSender(config);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(config.getUsuario());
        message.setTo(para);
        message.setSubject("Recuperação de Senha - LexCRM");
        message.setText("Olá,\n\nVocê solicitou a recuperação de senha no LexCRM.\n" +
                "Use o código abaixo para redefinir sua senha:\n\n" +
                token + "\n\n" +
                "Se você não solicitou isso, ignore este e-mail.");

        mailSender.send(message);
    }

    /**
     * Comprovante de assinatura paga (webhook). Não propaga exceção: falha de SMTP não deve invalidar o webhook.
     */
    public void enviarComprovantePagamentoAssinatura(
            String destinatario,
            Long tenantId,
            String nomeEmpresa,
            String nomePlano,
            BigDecimal valor,
            String gatewayRotulo,
            String referenciaPagamento
    ) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Comprovante por e-mail não enviado: destinatário ausente.");
            return;
        }
        Optional<EmailConfig> cfgOpt = buscarConfig(tenantId);
        if (cfgOpt.isEmpty()) {
            log.warn("Comprovante por e-mail não enviado: configuração SMTP não cadastrada para tenant {}.", resolverTenantId(tenantId));
            return;
        }
        EmailConfig config = cfgOpt.get();
        NumberFormat br = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        String valorTxt = valor != null ? br.format(valor) : "—";
        String ref = referenciaPagamento != null && !referenciaPagamento.isBlank() ? referenciaPagamento : "—";
        String corpo = "Olá,\n\n"
                + "Confirmamos o recebimento do pagamento da sua assinatura.\n\n"
                + "Empresa: " + (nomeEmpresa != null ? nomeEmpresa : "—") + "\n"
                + "Plano: " + (nomePlano != null ? nomePlano : "—") + "\n"
                + "Valor: " + valorTxt + "\n"
                + "Meio de pagamento: " + gatewayRotulo + "\n"
                + "Referência: " + ref + "\n\n"
                + "Guarde este e-mail como comprovante. O acesso ao sistema segue as regras do seu plano "
                + "(confirmação oficial pelo processamento no servidor).\n\n"
                + "Atenciosamente,\n"
                + "LexCRM";

        try {
            JavaMailSender mailSender = createMailSender(config);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getUsuario());
            message.setTo(destinatario.trim());
            message.setSubject("Comprovante de pagamento — LexCRM");
            message.setText(corpo);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar comprovante por e-mail para {}: {}", destinatario, e.getMessage());
        }
    }

    private EmailConfig buscarConfigObrigatoria(Long tenantId) {
        Long resolvedTenantId = resolverTenantId(tenantId);
        return buscarConfig(resolvedTenantId)
                .orElseThrow(() -> new RuntimeException("Configuração de e-mail não encontrada para este tenant. Configure o e-mail no módulo de Configurações."));
    }

    private Optional<EmailConfig> buscarConfig(Long tenantId) {
        Long resolvedTenantId = resolverTenantId(tenantId);
        if (resolvedTenantId == null) {
            return Optional.empty();
        }
        return configRepository.findByTenantId(resolvedTenantId);
    }

    private Long resolverTenantId(Long tenantId) {
        return tenantId != null ? tenantId : TenantContext.getCurrentTenant();
    }

    private JavaMailSender createMailSender(EmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsuario());
        mailSender.setPassword(config.getSenha());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", config.getProtocol());
        props.put("mail.smtp.auth", config.getAuth().toString());
        props.put("mail.smtp.starttls.enable", config.getStarttls().toString());
        props.put("mail.debug", "true");

        return mailSender;
    }
}
