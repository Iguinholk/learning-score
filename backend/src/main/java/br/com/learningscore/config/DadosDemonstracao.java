package br.com.learningscore.config;

import br.com.learningscore.agenda.CategoriaEvento;
import br.com.learningscore.agenda.EventoAgenda;
import br.com.learningscore.agenda.EventoAgendaRepository;
import br.com.learningscore.atividade.*;
import br.com.learningscore.conteudo.Conteudo;
import br.com.learningscore.conteudo.ConteudoRepository;
import br.com.learningscore.dadosacademicos.*;
import br.com.learningscore.usuario.Papel;
import br.com.learningscore.usuario.Usuario;
import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DADOS DE DEMONSTRAÇÃO — todos fictícios e identificados como tal (e-mails @learningscore.local, prefixo "[DEMO]").
 * Só é executado com learningscore.demo.enabled=true e apenas se o usuário demo ainda não existir.
 * Não representam dados reais de nenhuma instituição.
 */
@Component
@ConditionalOnProperty(name = "learningscore.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DadosDemonstracao implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);
    static final String EMAIL_ALUNO = "demo@learningscore.local";
    static final String EMAIL_PROFESSOR = "professor.demo@learningscore.local";

    private final UsuarioRepository usuarios;
    private final DisciplinaRepository disciplinas;
    private final NotaRepository notas;
    private final AtividadeRepository atividades;
    private final EventoAgendaRepository eventos;
    private final RegistroFinanceiroRepository financeiro;
    private final ConteudoRepository conteudos;
    private final PasswordEncoder encoder;

    @Value("${learningscore.demo.senha}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByEmailIgnoreCase(EMAIL_ALUNO)) return;

        Usuario prof = novoUsuario("[DEMO] Professora Exemplo", EMAIL_PROFESSOR, Papel.PROFESSOR);
        Usuario u = novoUsuario("[DEMO] Estudante Exemplo", EMAIL_ALUNO, Papel.ESTUDANTE);

        Disciplina poo = disciplina(u, "Programação Orientada a Objetos", "[DEMO] Prof. A", "java, classes, heranca, polimorfismo, encapsulamento", "88.00");
        Disciplina bd = disciplina(u, "Banco de Dados", "[DEMO] Prof. B", "sql, tabelas, chaves, normalizacao, consultas", "95.00");
        Disciplina es = disciplina(u, "Engenharia de Software", "[DEMO] Prof. C", "requisitos, uml, testes, scrum, arquitetura", "76.50");

        nota(poo, "Prova 1", "7.5", "2", -30); nota(poo, "Trabalho 1", "9.0", "1", -12);
        nota(bd, "Prova 1", "8.0", "2", -25); nota(bd, "Lista de exercícios", "10.0", "1", -10);
        nota(es, "Seminário", "6.5", "1", -20);

        LocalDateTime agora = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        atividade(u, poo, "Projeto final de POO", "Sistema com herança e polimorfismo.", agora.plusDays(6), Prioridade.ALTA, StatusAtividade.EM_ANDAMENTO, "https://exemplo.com/entrega/poo");
        atividade(u, bd, "Modelagem ER do trabalho", "Diagrama entidade-relacionamento.", agora.plusDays(1), Prioridade.ALTA, StatusAtividade.PENDENTE, null);
        atividade(u, es, "Documento de requisitos", "Levantar RF e RNF.", agora.plusDays(3), Prioridade.MEDIA, StatusAtividade.PENDENTE, null);
        atividade(u, bd, "Lista de SQL", "Consultas com JOIN.", agora.minusDays(2), Prioridade.MEDIA, StatusAtividade.PENDENTE, null);
        atividade(u, poo, "Exercícios de classes", "Capítulo 4.", agora.minusDays(5), Prioridade.BAIXA, StatusAtividade.CONCLUIDA, null);
        atividade(u, es, "Resumo de Scrum", "Uma página.", agora.plusDays(10), Prioridade.BAIXA, StatusAtividade.PENDENTE, null);

        evento(u, "Prova de Banco de Dados", LocalDate.now().plusDays(8), LocalTime.of(19, 0), CategoriaEvento.PROVA);
        evento(u, "Semana acadêmica", LocalDate.now().plusDays(14), null, CategoriaEvento.EVENTO_ACADEMICO);
        evento(u, "Reunião do grupo do projeto", LocalDate.now().plusDays(2), LocalTime.of(20, 30), CategoriaEvento.COMPROMISSO);

        financeiro(u, "Mensalidade (mês anterior)", LocalDate.now().minusMonths(1), "850.00", LocalDate.now().minusDays(20), true);
        financeiro(u, "Mensalidade (mês atual)", LocalDate.now(), "850.00", LocalDate.now().plusDays(7), false);
        financeiro(u, "Mensalidade (próximo mês)", LocalDate.now().plusMonths(1), "850.00", LocalDate.now().plusDays(37), false);

        if (conteudos.count() == 0) {
            // 5 palavras por disciplina: compatibilidade = comuns/5. Conteúdos de 4+ palavras (≥80%) são elegíveis.
            conteudo("Fundamentos de orientação a objetos em Java", "java, classes, heranca, polimorfismo, encapsulamento", true, prof);
            conteudo("Herança e polimorfismo na prática", "java, classes, heranca, polimorfismo", true, prof);
            conteudo("Classes e encapsulamento", "java, classes, polimorfismo, encapsulamento", true, prof);
            conteudo("Introdução a Java (iniciante)", "java, variaveis", true, prof);               // 20% → não recomendado
            conteudo("Modelagem relacional e normalização", "sql, tabelas, chaves, normalizacao, consultas", true, prof);
            conteudo("Consultas SQL avançadas", "sql, tabelas, chaves, consultas", true, prof);
            conteudo("Chaves e integridade referencial", "sql, tabelas, chaves, normalizacao", true, prof);
            conteudo("Levantamento de requisitos", "requisitos, uml, testes, scrum, arquitetura", true, prof);
            conteudo("Diagramas UML essenciais", "requisitos, uml, testes, arquitetura", true, prof);
            conteudo("Testes de software e arquitetura", "requisitos, testes, scrum, arquitetura", true, prof);
            conteudo("[Aguardando aprovação] Java completo", "java, classes, heranca, polimorfismo, encapsulamento", false, null);
        }
        log.info("Dados de DEMONSTRAÇÃO criados. Login: {} / (senha definida em DEMO_PASSWORD). Professor: {}", EMAIL_ALUNO, EMAIL_PROFESSOR);
    }

    private Usuario novoUsuario(String nome, String email, Papel papel) {
        Usuario u = new Usuario();
        u.setNome(nome); u.setEmail(email); u.setPapel(papel); u.setSenhaHash(encoder.encode(senha));
        return usuarios.save(u);
    }

    private Disciplina disciplina(Usuario u, String nome, String prof, String palavras, String freq) {
        Disciplina d = new Disciplina();
        d.setUsuario(u); d.setNome(nome); d.setProfessor(prof); d.setPalavrasChave(palavras);
        d.setFrequenciaPercentual(new BigDecimal(freq));
        return disciplinas.save(d);
    }

    private void nota(Disciplina d, String desc, String valor, String peso, int diasAtras) {
        Nota n = new Nota();
        n.setDisciplina(d); n.setDescricao(desc); n.setValor(new BigDecimal(valor)); n.setPeso(new BigDecimal(peso));
        n.setDataLancamento(LocalDate.now().plusDays(diasAtras));
        notas.save(n);
    }

    private void atividade(Usuario u, Disciplina d, String titulo, String desc, LocalDateTime prazo, Prioridade p, StatusAtividade s, String link) {
        Atividade a = new Atividade();
        a.setUsuario(u); a.setDisciplina(d); a.setTitulo(titulo); a.setDescricao(desc); a.setPrazo(prazo);
        a.setPrioridade(p); a.setLinkEntrega(link);
        a.setStatus(AtividadeService.normalizarStatus(s, prazo));
        atividades.save(a);
    }

    private void evento(Usuario u, String titulo, LocalDate data, LocalTime hora, CategoriaEvento c) {
        EventoAgenda e = new EventoAgenda();
        e.setUsuario(u); e.setTitulo(titulo); e.setData(data); e.setHorario(hora); e.setCategoria(c);
        eventos.save(e);
    }

    private void financeiro(Usuario u, String desc, LocalDate comp, String valor, LocalDate venc, boolean pago) {
        RegistroFinanceiro r = new RegistroFinanceiro();
        r.setUsuario(u); r.setDescricao(desc); r.setCompetencia(comp.withDayOfMonth(1)); r.setValor(new BigDecimal(valor));
        r.setVencimento(venc);
        r.setStatus(pago ? StatusFinanceiro.PAGO : StatusFinanceiro.PENDENTE);
        r.setDataPagamento(pago ? venc.minusDays(2) : null);
        financeiros(r);
    }
    private void financeiros(RegistroFinanceiro r) { financeiro.save(r); }

    private void conteudo(String titulo, String palavras, boolean aprovado, Usuario prof) {
        Conteudo c = new Conteudo();
        c.setTitulo("[DEMO] " + titulo);
        c.setDescricao("Conteúdo fictício de demonstração (sem vídeo). Cadastre um link real do YouTube em Conteúdos para testar o mini-player.");
        c.setPalavrasChave(palavras);
        c.setAprovado(aprovado);
        c.setStatusAvaliacao(aprovado ? br.com.learningscore.conteudo.StatusAvaliacao.APROVADO : br.com.learningscore.conteudo.StatusAvaliacao.PENDENTE);
        c.setAprovadoPor(aprovado && prof != null ? prof.getNome() : null);
        conteudos.save(c);
    }
}
