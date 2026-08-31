package com.market.order.internal.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/order")
@Tag(name = "Orders", description = "Gerenciamento de pedidos")
public interface OrderHttpApi {

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    @Operation(summary = "Cria um novo pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido recebido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do pedido inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    ResponseEntity<String> createOrder(@Valid @RequestBody CreateOrderRequest request);
}
