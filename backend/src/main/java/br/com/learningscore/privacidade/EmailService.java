package br.com.learningscore.privacidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envia e-mails somente se o servidor SMTP estiver configurado (SPRING_MAIL_HOST). Caso contrário, apenas registra no log. */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> sender;
    private final String remetente;

    public EmailService(ObjectProvider<JavaMailSender> sender, @Value("${learningscore.mail.from}") String remetente) {
        this.sender = sender;
        this.remetente = remetente;
    }

    /** Reaproveita o SMTP existente; inclui versão texto para clientes sem HTML. */
    public boolean enviarHtml(String para, String assunto, String texto, String html) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) return false;
        try {
            var mensagem = s.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(texto, html);
            s.send(mensagem);
            return true;
        } catch (Exception e) {
            // Não registra destinatário, corpo, links ou tokens.
            log.warn("Falha ao enviar e-mail HTML ({})", e.getClass().getSimpleName());
            return false;
        }
    }

    /** @return true se o e-mail foi enviado; false se o serviço não está configurado ou falhou. */
    public boolean enviar(String para, String assunto, String texto) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.info("E-mail NÃO enviado (SMTP não configurado): assunto='{}'", assunto);
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(remetente);
            m.setTo(para);
            m.setSubject(assunto);
            m.setText(texto);
            s.send(m);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail: {}", e.getMessage());
            return false;
        }
    }
}
