package br.com.learningscore.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tratamento centralizado de exceções: nunca expõe stack trace ao cliente. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        var corpo = new ErroApi(400, "Dados inválidos", "Corrija os campos indicados.", campos, LocalDateTime.now());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class})
    public ResponseEntity<ErroApi> requisicaoInvalida(Exception ex) {
        return ResponseEntity.badRequest().body(ErroApi.de(400, "Requisição inválida", "Formato de dados incorreto."));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErroApi> semRecurso(Exception ex) {
        return ResponseEntity.status(404).body(ErroApi.de(404, "Não encontrado", "Recurso inexistente."));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroApi> metodo(Exception ex) {
        return ResponseEntity.status(405).body(ErroApi.de(405, "Método não permitido", "Método HTTP não suportado."));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroApi> naoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(404).body(ErroApi.de(404, "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroApi> regra(RegraNegocioException ex) {
        return ResponseEntity.status(422).body(ErroApi.de(422, "Regra de negócio", ex.getMessage()));
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ErroApi> limite(LimiteExcedidoException ex) {
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosRestantes()))
                .body(ErroApi.de(429, "Limite excedido", ex.getMessage()));
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ErroApi> credenciais(Exception ex) {
        // mensagem genérica: não revela se o e-mail existe
        return ResponseEntity.status(401).body(ErroApi.de(401, "Não autorizado", "E-mail ou senha inválidos."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApi> negado(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ErroApi.de(403, "Acesso negado", "Você não tem permissão para esta ação."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroApi.de(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }
}
