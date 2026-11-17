package br.com.learningscore.conteudo;

import br.com.learningscore.common.RegraNegocioException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Texto livre é exibido como texto; somente URLs http(s) validadas viram links. Nenhuma URL é buscada pelo servidor. */
public final class LinksAvaliacao {
    private LinksAvaliacao() {}
    private static final Pattern URL = Pattern.compile("(?i)(?:https?://|(?:javascript|data|vbscript|file|ftp)\\s*:)[^\\s<>\\x22\\x27]*");
    public static List<String> extrair(String texto) {
        List<String> links = new ArrayList<>();
        if (texto == null) return links;
        var matcher = URL.matcher(texto);
        while (matcher.find()) {
            String link = matcher.group().replaceAll("[.,;!?)\\]]+$", "");
            try {
                URI u = URI.create(link);
                if (!("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme()))
                        || u.getHost() == null || u.getUserInfo() != null || link.length() > 2000) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                throw new RegraNegocioException("Use apenas links completos http ou https, sem usuário ou senha, na justificativa.");
            }
            if (!links.contains(link)) links.add(link);
        }
        if (links.size() > 20) throw new RegraNegocioException("Informe no máximo 20 links na justificativa.");
        return List.copyOf(links);
    }
}
