package com.kodlamaio.commonpackage.configuration.exceptions;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// NOTE: each handler method returns a plain ExceptionResult<Object>, not a ResponseEntity.
// The HTTP status comes from the method's @ResponseStatus annotation, which Spring's
// dispatcher machinery applies only when the handler runs inside a live MVC context.
// Calling the method directly (no @SpringBootTest / MockMvc here per the no-Spring-context
// rule) returns only the body, so the status is verified via the annotation itself.
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private static HttpStatus responseStatusOf(String methodName, Class<?> paramType) throws NoSuchMethodException {
        return RestExceptionHandler.class.getMethod(methodName, paramType)
                .getAnnotation(ResponseStatus.class).value();
    }

    @Test
    void handleMethodArgumentNotValidException_returns400WithFieldErrorsMap() throws NoSuchMethodException {
        var bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("objectName", "fieldName", "must not be blank")));
        var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var result = handler.handleMethodArgumentNotValidException(exception);

        assertThat(result.getType()).isEqualTo("VALIDATION_EXCEPTION");
        assertThat(result.getMessage()).isEqualTo(java.util.Map.of("fieldName", "must not be blank"));
        assertThat(responseStatusOf("handleMethodArgumentNotValidException", MethodArgumentNotValidException.class))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidationException_returns422WithExceptionMessage() throws NoSuchMethodException {
        var exception = new ValidationException("bad input");

        var result = handler.handleValidationException(exception);

        assertThat(result.getType()).isEqualTo("VALIDATION_EXCEPTION");
        assertThat(result.getMessage()).isEqualTo("bad input");
        assertThat(responseStatusOf("handleValidationException", ValidationException.class))
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleBusinessException_returns422WithExceptionMessage() throws NoSuchMethodException {
        var exception = new BusinessException("some business rule failed");

        var result = handler.handleBusinessException(exception);

        assertThat(result.getType()).isEqualTo("BUSINESS_EXCEPTION");
        assertThat(result.getMessage()).isEqualTo("some business rule failed");
        assertThat(responseStatusOf("handleBusinessException", BusinessException.class))
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleDataIntegrityViolation_returns409WithExceptionMessage() throws NoSuchMethodException {
        var exception = new DataIntegrityViolationException("duplicate key");

        var result = handler.handleDataIntegrityViolation(exception);

        assertThat(result.getType()).isEqualTo("DATA_INTEGRITY_VIOLATION_EXCEPTION");
        assertThat(result.getMessage()).isEqualTo("duplicate key");
        assertThat(responseStatusOf("handleDataIntegrityViolation", DataIntegrityViolationException.class))
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleRuntimeException_returns500WithExceptionMessage() throws NoSuchMethodException {
        var exception = new RuntimeException("unexpected");

        var result = handler.handleRuntimeException(exception);

        assertThat(result.getType()).isEqualTo("RUNTIME_EXCEPTION");
        assertThat(result.getMessage()).isEqualTo("unexpected");
        assertThat(responseStatusOf("handleRuntimeException", RuntimeException.class))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
