package br.com.learningscore.conteudo;
import br.com.learningscore.privacidade.EmailService;
import br.com.learningscore.usuario.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class ConteudoEmailServiceTest {
    @Test @SuppressWarnings("unchecked") void reutilizaSenderETrataFalhaSemExporConteudo() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenAnswer(i -> new JavaMailSenderImpl().createMimeMessage());
        var service = new EmailService(provider, "noreply@example.test");
        assertTrue(service.enviarHtml("prof@example.test", "Avaliar", "Texto", "<p>Texto</p>"));
        verify(sender).send(any(MimeMessage.class));
        doThrow(new IllegalStateException("falha smtp")).when(sender).send(any(MimeMessage.class));
        assertFalse(service.enviarHtml("prof@example.test", "Avaliar", "Texto", "<p>Texto</p>"));
        when(provider.getIfAvailable()).thenReturn(null);
        assertFalse(service.enviarHtml("prof@example.test", "Avaliar", "Texto", "<p>Texto</p>"));
    }
    @Test void enderecoPublicoExigeHttpsForaDeLocalhost() {
        var repo = mock(ConteudoRepository.class); var users = mock(UsuarioRepository.class); var mail = mock(EmailService.class);
        assertDoesNotThrow(() -> new ConteudoEmailService(repo, users, mail, "https://learning.example.test").validarEndereco());
        assertDoesNotThrow(() -> new ConteudoEmailService(repo, users, mail, "http://localhost:8080").validarEndereco());
        assertThrows(IllegalStateException.class, () -> new ConteudoEmailService(repo, users, mail, "http://learning.example.test").validarEndereco());
        assertThrows(IllegalStateException.class, () -> new ConteudoEmailService(repo, users, mail, "https://user:pass@example.test").validarEndereco());
    }
}
