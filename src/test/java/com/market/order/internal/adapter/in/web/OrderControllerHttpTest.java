package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OrderControllerHttpTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CreateOrderUseCase useCase = command -> new CreateOrderResult(UUID.randomUUID());
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = standaloneSetup(new OrderController(useCase))
                .setValidator(validator)
                .build();
    }

    @Test
    void preservesTheCreatedHttpContract() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "550e8400-e29b-41d4-a716-446655440000",
                                  "paymentMethod": "PIX",
                                  "items": [
                                    {
                                      "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Pedido recebido com sucesso"));
    }

    @Test
    void rejectsAnInvalidRequestBeforeCallingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "550e8400-e29b-41d4-a716-446655440000",
                                  "paymentMethod": " ",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
