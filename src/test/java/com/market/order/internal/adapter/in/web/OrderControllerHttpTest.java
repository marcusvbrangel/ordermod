package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.exception.OrderNotFoundException;
import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.in.GetOrderResult;
import com.market.order.internal.application.port.in.GetOrderUseCase;
import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OrderControllerHttpTest {

    private static final UUID ORDER_ID = UUID.fromString("fb116546-49d5-4946-86e2-a18327817eb9");
    private static final UUID CUSTOMER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID ITEM_ID = UUID.fromString("384414fd-8b64-44df-8678-304f108f87f7");
    private static final UUID PRODUCT_ID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    private static final Instant CREATED_AT = Instant.parse("2026-09-02T12:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CreateOrderUseCase createOrderUseCase = command -> new CreateOrderResult(
                ORDER_ID,
                "AGUARDANDO_ESTOQUE",
                new BigDecimal("21.00"),
                "BRL",
                List.of(new CreateOrderResult.Item(
                        PRODUCT_ID,
                        2,
                        new BigDecimal("10.50"),
                        new BigDecimal("21.00")
                ))
        );
        GetOrderUseCase getOrderUseCase = query -> orderResult();
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = standaloneSetup(new OrderController(createOrderUseCase, getOrderUseCase))
                .setControllerAdvice(new OrderExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsACompleteOrderByIdAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/order/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ESTOQUE"))
                .andExpect(jsonPath("$.totalAmount").value(21.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-02T12:00:00Z"))
                .andExpect(jsonPath("$.items[0].itemId").value(ITEM_ID.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(21.00));
    }

    @Test
    void returnsNotFoundProblemWhenOrderDoesNotExist() throws Exception {
        GetOrderUseCase missingOrderUseCase = query -> {
            throw new OrderNotFoundException(query.orderId());
        };
        var missingOrderMockMvc = mockMvcWith(missingOrderUseCase);

        missingOrderMockMvc.perform(get("/api/v1/order/{orderId}", ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Pedido não encontrado: " + ORDER_ID));
    }

    @Test
    void rejectsMalformedOrderIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/order/{orderId}", "identificador-invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheCalculatedCommercialSnapshotAsJson() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ESTOQUE"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.totalAmount").value(21.00))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(21.00));
    }

    @Test
    void rejectsMissingCurrencyAndUnitPrice() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "550e8400-e29b-41d4-a716-446655440000",
                                  "paymentMethod": "CREDIT_CARD",
                                  "items": [{
                                    "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                                    "quantity": 2
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveOrExcessivelyPreciseUnitPrice() throws Exception {
        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("10.50", "0.00")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("10.50", "10.501")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsDomainValidationFailureToBadRequest() throws Exception {
        CreateOrderUseCase rejectingUseCase = command -> {
            throw new OrderDomainException("currency inválida: XYZ");
        };
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var validatingMockMvc = standaloneSetup(new OrderController(rejectingUseCase, query -> orderResult()))
                .setControllerAdvice(new OrderExceptionHandler())
                .setValidator(validator)
                .build();

        validatingMockMvc.perform(post("/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("BRL", "XYZ")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("currency inválida: XYZ"));
    }

    private static MockMvc mockMvcWith(GetOrderUseCase getOrderUseCase) {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        CreateOrderUseCase unusedCreateOrderUseCase = command -> {
            throw new AssertionError("criação não deveria ser chamada");
        };

        return standaloneSetup(new OrderController(unusedCreateOrderUseCase, getOrderUseCase))
                .setControllerAdvice(new OrderExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static GetOrderResult orderResult() {
        return new GetOrderResult(
                ORDER_ID,
                CUSTOMER_ID,
                "PIX",
                "AGUARDANDO_ESTOQUE",
                new BigDecimal("21.00"),
                "BRL",
                CREATED_AT,
                List.of(new GetOrderResult.Item(
                        ITEM_ID,
                        PRODUCT_ID,
                        2,
                        new BigDecimal("10.50"),
                        new BigDecimal("21.00")
                ))
        );
    }

    private static String validRequest() {
        return """
                {
                  "customerId": "550e8400-e29b-41d4-a716-446655440000",
                  "paymentMethod": "CREDIT_CARD",
                  "currency": "BRL",
                  "items": [{
                    "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                    "quantity": 2,
                    "unitPrice": 10.50
                  }]
                }
                """;
    }
}
