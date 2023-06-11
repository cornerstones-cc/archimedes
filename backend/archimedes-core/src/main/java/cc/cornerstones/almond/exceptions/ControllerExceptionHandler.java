package cc.cornerstones.almond.exceptions;

import cc.cornerstones.almond.types.Response;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

@ControllerAdvice
@ResponseBody
public class ControllerExceptionHandler implements ResponseBodyAdvice<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @Autowired
    private MessageSourceComponent messageSourceComponent;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response handleException(Exception exception, Locale locale) {
        ErrorCode errorCode = null;
        String[] errorParams = null;
        String basicErrorMessage = null;
        String additionalErrorMessage = null;
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException specificException = (MethodArgumentNotValidException) exception;
            if (specificException.hasErrors()) {
                StringBuilder message = new StringBuilder();
                specificException.getAllErrors().forEach(objectError -> {
                    if (!ObjectUtils.isEmpty(objectError.getDefaultMessage())) {
                        if (message.length() > 0) {
                            message.append("\n");
                        }
                        message.append(objectError.getDefaultMessage());
                    }
                });
                additionalErrorMessage = message.toString();
            }

            errorCode = ErrorCode.GENERAL_ILLEGAL_PARAMETER_ERROR;
        } else if (exception instanceof AbcUndefinedException) {
            errorCode = ((AbcUndefinedException) exception).getErrorCode();
            errorParams = ((AbcUndefinedException) exception).getErrorParams();

            // 取前15行 Stack Trace
            StackTraceElement[] stackTraceElementArray = exception.getStackTrace();
            if (stackTraceElementArray.length > 15) {
                StackTraceElement[] cutStackTraceElementArray = new StackTraceElement[15];

                for (int i = 0; i < 15; i++) {
                    cutStackTraceElementArray[i] = stackTraceElementArray[i];
                }

                exception.setStackTrace(cutStackTraceElementArray);
            }

            LOGGER.error(exception.getMessage(), exception);

            basicErrorMessage = exception.getMessage();
        } else {
            errorCode = ErrorCode.GENERAL_UNDEFINED_ERROR;
            LOGGER.error(exception.getMessage(), exception);

            basicErrorMessage = exception.getMessage();
        }
        return buildResponse(errorCode, errorParams, basicErrorMessage, additionalErrorMessage);
    }

    /**
     * IO 错误
     *
     * @param exception
     * @param locale
     * @return
     */
    @ExceptionHandler(IOException.class)
    @ResponseBody
    public Response handleIOException(Exception exception, Locale locale) {
        if (exception instanceof ClientAbortException) {
            LOGGER.error(exception.getMessage());
            return null;
        }
        LOGGER.error(exception.getMessage(), exception);
        ErrorCode errorCode = ErrorCode.GENERAL_IO_ERROR;
        return buildResponse(errorCode,null, "IO error",null);
    }

    /**
     * SQL 错误
     *
     * @param exception
     * @param locale
     * @return
     */
    @ExceptionHandler(SQLException.class)
    @ResponseBody
    public Response handleSQLException(Exception exception, Locale locale) {
        LOGGER.error(exception.getMessage(), exception);
        ErrorCode errorCode = ErrorCode.GENERAL_SQL_ERROR;
        return buildResponse(errorCode, null, "SQL error",null);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter methodParameter,
            MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> aClass,
            ServerHttpRequest serverHttpRequest,
            ServerHttpResponse serverHttpResponse) {
        if (body instanceof Response) {
            Response bodyResponse = (Response) body;
            if (!bodyResponse.isSuccessful()) {
                if (bodyResponse.getErrCode() != null) {
                    switch (bodyResponse.getErrCode()) {
                        // AUTH_AUTHENTICATION_ERROR
                        // AUTH_UNAUTHORIZED_ERROR
                        case 10110:
                        case 10116:
                            serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
                            break;
                        default:
                            serverHttpResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                            break;
                    }
                } else {
                    serverHttpResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return body;
    }

    private Response buildResponse(
            ErrorCode errorCode,
            String[] errorParams,
            String basicErrorMessage,
            String additionalErrorMessage) {
        StringBuilder errorMessage = new StringBuilder();

        String message = this.messageSourceComponent.getMessage(errorCode.getCodeSymbol(), errorParams);
        if (!ObjectUtils.isEmpty(message)) {
            errorMessage.append("[").append(message).append("]");
        }
        if (!ObjectUtils.isEmpty(basicErrorMessage)) {
            errorMessage.append("[").append(basicErrorMessage).append("]");
        }
        if ((!ObjectUtils.isEmpty(additionalErrorMessage))) {
            errorMessage.append("[").append(additionalErrorMessage).append("]");
        }
        return Response.buildFailure(errorCode.getCode(), errorMessage.toString());
    }

}