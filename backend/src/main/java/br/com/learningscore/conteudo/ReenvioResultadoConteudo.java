package br.com.learningscore.conteudo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class ReenvioResultadoConteudo {
    private final ConteudoRepository repo;
    private final ConteudoEmailService email;
    @Scheduled(fixedDelay = 600_000, initialDelay = 90_000)
    public void reenviarResultados() {
        for (Long id : repo.resultadosSemEmail(PageRequest.of(0, 50))) email.enviarResultado(id);
    }
}
