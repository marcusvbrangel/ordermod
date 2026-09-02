package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.exception.OrderNotCancellableException;
import com.market.order.internal.application.exception.OrderNotFoundException;
import com.market.order.internal.domain.exception.OrderDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFoundException(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrderDomainException.class)
    ProblemDetail handleDomainException(OrderDomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    ProblemDetail handleNotCancellable(OrderNotCancellableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
