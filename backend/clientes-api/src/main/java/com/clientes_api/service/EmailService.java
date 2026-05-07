package com.clientes_api.service;

import com.clientes_api.model.EmailConfig;
import com.clientes_api.repository.EmailConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    @Autowired
    private EmailConfigRepository configRepository;

    public void enviarEmailRecuperacao(String para, String token) {
        EmailConfig config = configRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Configuração de e-mail não encontrada. Configure o e-mail no módulo de Configurações."));

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
