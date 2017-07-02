package example.ark_smartbridge_listener;

import lombok.extern.log4j.Log4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j
@RestControllerAdvice
public class ErrorController {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorView notFound(NotFoundException e) {
        ErrorView errorView = new ErrorView();
        errorView.setMessage(e.getMessage());
        return errorView;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorView error(Exception e) {
        log.error("Unhandled exception thrown", e);
        ErrorView errorView = new ErrorView();
        errorView.setMessage("An error occurred. Please try again later.");
        return errorView;
    }
}
