package br.com.learningscore.conteudo;

import br.com.learningscore.common.RegraNegocioException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConteudoServiceTest {
    @Test
    void converteLinkDoYoutube() {
        assertEquals("https://www.youtube-nocookie.com/embed/abcdefghijk",
                ConteudoService.paraEmbed("https://www.youtube.com/watch?v=abcdefghijk"));
        assertEquals("https://www.youtube-nocookie.com/embed/abcdefghijk",
                ConteudoService.paraEmbed("https://youtu.be/abcdefghijk"));
    }

    @Test
    void rejeitaDominiosNaoPermitidos() {
        assertThrows(RegraNegocioException.class, () -> ConteudoService.paraEmbed("https://site-malicioso.com/video"));
        assertThrows(RegraNegocioException.class, () -> ConteudoService.paraEmbed("javascript:alert(1)"));
    }

    @Test
    void urlVaziaEhPermitida() {
        assertNull(ConteudoService.paraEmbed("  "));
    }
}
