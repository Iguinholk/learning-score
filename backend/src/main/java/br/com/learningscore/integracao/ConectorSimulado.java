package br.com.learningscore.integracao;

import org.springframework.stereotype.Component;

/** SIMULAÇÃO: não acessa nenhum serviço externo e NÃO importa dados. Serve para demonstrar o fluxo de sincronização. */
@Component
public class ConectorSimulado implements ConectorPlataforma {
    @Override
    public boolean suporta(PlataformaExterna plataforma) { return plataforma.isSimulada(); }

    @Override
    public String sincronizar(PlataformaExterna plataforma) throws InterruptedException {
        Thread.sleep(400);
        return "SIMULAÇÃO: fluxo executado, nenhum dado real foi importado de \"" + plataforma.getNome() + "\".";
    }
}
