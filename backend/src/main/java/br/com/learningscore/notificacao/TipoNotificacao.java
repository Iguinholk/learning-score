package br.com.learningscore.notificacao;

public enum TipoNotificacao {
    PRAZO_PROXIMO("Prazos próximos"),
    ATIVIDADE_ATRASADA("Atividades atrasadas"),
    EVENTO_FUTURO("Eventos futuros"),
    ATUALIZACAO_ACADEMICA("Atualizações acadêmicas");

    private final String rotulo;
    TipoNotificacao(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
