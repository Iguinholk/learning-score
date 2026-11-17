package br.com.learningscore.usuario;

import br.com.learningscore.privacidade.ConsentimentoService;
import br.com.learningscore.privacidade.TipoConsentimento;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock UsuarioRepository repo;
    @Mock PasswordEncoder encoder;
    @Mock ConsentimentoService consentimentos;
    @InjectMocks UsuarioService service;

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void registraEscolhasIndependentesNoCadastro(boolean integracoes, boolean dados) {
        when(encoder.encode("Senha1234")).thenReturn("hash");
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        service.registrar(new UsuarioDtos.RegistroRequest(
                "Estudante", "aluno@example.com", "Senha1234", true, integracoes, dados));
        verify(consentimentos).registrar(7L, TipoConsentimento.TERMOS_USO, true);
        verify(consentimentos).registrar(7L, TipoConsentimento.INTEGRACOES, integracoes);
        verify(consentimentos).registrar(7L, TipoConsentimento.USO_DADOS_EXTERNOS, dados);
    }
}
