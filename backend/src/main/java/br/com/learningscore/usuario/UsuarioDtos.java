package br.com.learningscore.usuario;

import jakarta.validation.constraints.*;

public final class UsuarioDtos {
    private UsuarioDtos() {}

    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha,
            @AssertTrue(message = "É necessário aceitar os termos de uso e a política de privacidade") boolean aceitouTermos,
            boolean consentiuIntegracoes,
            boolean consentiuUsoDadosExternos) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    public record UsuarioResponse(Long id, String nome, String email, String papel) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel().name());
        }
    }
}
