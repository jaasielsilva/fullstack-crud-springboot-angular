package com.clientes_api.controller;

import com.clientes_api.dto.CheckoutRequestDTO;
import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.dto.PlanoResponseDTO;
import com.clientes_api.model.Plano;
import com.clientes_api.model.Usuario;
import com.clientes_api.service.CheckoutMercadoPagoService;
import com.clientes_api.service.PlanoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Checkout público", description = "Planos e checkout Mercado Pago (backend seguro)")
public class PublicCheckoutController {

    private final PlanoService planoService;
    private final CheckoutMercadoPagoService checkoutMercadoPagoService;

    public PublicCheckoutController(PlanoService planoService,
                                    CheckoutMercadoPagoService checkoutMercadoPagoService) {
        this.planoService = planoService;
        this.checkoutMercadoPagoService = checkoutMercadoPagoService;
    }

    @GetMapping("/planos")
    @Operation(summary = "Lista planos ativos disponíveis para contratação")
    public ResponseEntity<List<PlanoResponseDTO>> listarPlanos() {
        List<PlanoResponseDTO> lista = planoService.listarAtivosOrdenados().stream()
                .map(PublicCheckoutController::mapear)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Cria preferência Mercado Pago e devolve URL de checkout")
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @Valid @RequestBody CheckoutRequestDTO body,
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(checkoutMercadoPagoService.criarCheckout(body, usuario));
    }

    private static PlanoResponseDTO mapear(Plano p) {
        return new PlanoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getValor(),
                p.getTipo().name()
        );
    }
}
