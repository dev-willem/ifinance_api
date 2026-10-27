package com.willembergfilho.ifinance.api.advice;

import com.willembergfilho.ifinance.application.investment.InvestmentNotFoundException;
import com.willembergfilho.ifinance.application.simulation.SimulationNotFoundException;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.willembergfilho.ifinance.api")
public class GlobalExceptionHandler {

    @ExceptionHandler({SimulationNotFoundException.class, InvestmentNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(CalculationException.class)
    public ResponseEntity<ApiError> handleCalculation(CalculationException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "Calculation Error", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Validation Error", message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Malformed Request Body",
                "Corpo da requisição inválido ou malformado.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        Class<?> type = ex.getRequiredType();
        String message = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue() + ".";
        if (type != null && type.isEnum()) {
            message += " Valores aceitos: " + Arrays.stream(type.getEnumConstants())
                    .map(c -> ((Enum<?>) c).name()).collect(Collectors.joining(", ")) + ".";
        }
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request",
                "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'.", req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Validation Error", "Parâmetros da requisição inválidos.", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurity(SecurityException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.", req);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String error,
                                           String message, HttpServletRequest req) {
        ApiError body = new ApiError(status.value(), error, message, req.getRequestURI());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
