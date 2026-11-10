package br.com.learningscore.conteudo;

import br.com.learningscore.common.RecursoNaoEncontradoException;
import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.conteudo.ConteudoDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** RF09/RF11: catálogo de conteúdos, sugestão por estudantes e aprovação por professores. */
@Service
@RequiredArgsConstructor
public class ConteudoService {
    private static final Pattern YOUTUBE = Pattern.compile(
            "^https://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})(?:[?&#].*)?$");
    private static final Pattern VIMEO = Pattern.compile("^https://(?:www\\.)?vimeo\\.com/(\\d{6,12})(?:[/?#].*)?$");

    private final ConteudoRepository repo;

    /** Converte links comuns em URL de incorporação segura. Aceita apenas YouTube e Vimeo (lista de permissão). */
    public static String paraEmbed(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        Matcher y = YOUTUBE.matcher(u);
        if (y.matches()) return "https://www.youtube-nocookie.com/embed/" + y.group(1);
        Matcher v = VIMEO.matcher(u);
        if (v.matches()) return "https://player.vimeo.com/video/" + v.group(1);
        throw new RegraNegocioException("Use um link https do YouTube ou do Vimeo.");
    }

    @Transactional(readOnly = true)
    public List<Response> aprovados() {
        return repo.findByAprovadoTrueOrderByTituloAsc().stream().map(Response::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Response> pendentes(Long uid, boolean professor) {
        List<Conteudo> lista = professor ? repo.findByAprovadoFalseOrderByCriadoEmDesc()
                                         : new ArrayList<>(repo.findBySugeridoPorAndAprovadoFalse(uid));
        return lista.stream().map(Response::de).toList();
    }

    @Transactional
    public Response sugerir(Long uid, Request r) {
        Conteudo c = new Conteudo();
        c.setTitulo(r.titulo().trim());
        c.setDescricao(r.descricao());
        c.setUrlEmbed(paraEmbed(r.urlVideo()));
        c.setPalavrasChave(r.palavrasChave() == null ? "" : r.palavrasChave().trim());
        c.setSugeridoPor(uid);
        c.setAprovado(false);                       // só professores aprovam
        return Response.de(repo.save(c));
    }

    @Transactional
    public Response aprovar(Long id, String nomeProfessor) {
        Conteudo c = repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado"));
        c.setAprovado(true);
        c.setAprovadoPor(nomeProfessor);
        return Response.de(c);
    }
}
