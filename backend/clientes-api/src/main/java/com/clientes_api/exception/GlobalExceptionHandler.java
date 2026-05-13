package com.clientes_api.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.FORBIDDEN.value());
        erro.put("erro", ex.getMessage() != null ? ex.getMessage() : "Acesso negado");
        erro.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.UNAUTHORIZED.value());
        erro.put("erro", ex.getMessage() != null ? ex.getMessage() : "Não autenticado");
        erro.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    /**
     * Erros de persistência (JPA/Hibernate), com mensagem amigável quando o stack técnico não ajuda o usuário final.
     */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceException(
            PersistenceException ex,
            HttpServletRequest request) {

        String mensagemUsuario = "Não foi possível salvar os dados. Verifique as informações e tente novamente.";
        Throwable t = ex;
        while (t != null) {
            String m = t.getMessage();
            if (m != null) {
                if (m.contains("orphan") || m.contains("orphanRemoval")) {
                    mensagemUsuario = "Não foi possível atualizar os itens do pedido. Recarregue a página e tente novamente.";
                    break;
                }
                if (m.contains("constraint") || m.contains("foreign key") || m.contains("Duplicate")) {
                    mensagemUsuario = "Esta operação conflita com outros dados no sistema (integridade). Verifique e tente novamente.";
                    break;
                }
            }
            t = t.getCause();
        }

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.BAD_REQUEST.value());
        erro.put("erro", mensagemUsuario);
        erro.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(erro);
    }

    /**
     * Hibernate costuma lançar IllegalStateException em falhas de coleção (ex.: orphanRemoval).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {

        String mensagemUsuario = "Operação não pôde ser concluída. Tente novamente.";
        String m = ex.getMessage();
        if (m != null && (m.contains("orphan") || m.contains("orphanRemoval") || m.contains("referenced by the owning entity"))) {
            mensagemUsuario = "Não foi possível atualizar os itens do pedido. Recarregue a página e tente novamente.";
        }

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.BAD_REQUEST.value());
        erro.put("erro", mensagemUsuario);
        erro.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.BAD_REQUEST.value());
        erro.put("erro", ex.getMessage() != null ? ex.getMessage() : "Ocorreu um erro inesperado. Tente novamente.");
        erro.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> campos = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            campos.put(error.getField(), error.getDefaultMessage());
        });

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.BAD_REQUEST.value());
        erro.put("erro", "Erro de validação");
        erro.put("campos", campos);
        erro.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.NOT_FOUND.value());
        erro.put("erro", ex.getMessage());
        erro.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        Map<String, Object> erro = new HashMap<>();
        erro.put("timestamp", LocalDateTime.now());
        erro.put("status", HttpStatus.BAD_REQUEST.value());
        erro.put("erro", ex.getMessage());
        erro.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(erro);
    }
}
