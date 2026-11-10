package br.com.learningscore.integracao;

/**
 * Ponto de extensão para integrações reais (OAuth 2.0 / APIs de AVAs).
 * Para integrar uma plataforma de verdade: crie uma classe @Component que implemente esta interface,
 * obtenha o token OAuth 2.0 do estudante (com consentimento) e importe os dados.
 * NÃO existem credenciais nem APIs reais neste projeto.
 */
public interface ConectorPlataforma {
    boolean suporta(PlataformaExterna plataforma);

    /** @return mensagem de resultado para o histórico. Lança exceção em caso de falha. */
    String sincronizar(PlataformaExterna plataforma) throws Exception;
}
