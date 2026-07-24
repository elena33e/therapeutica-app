package com.therapeutica.therapeutica_app.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesInterzisException extends RuntimeException {

    public AccesInterzisException() {
        super();
    }

    public AccesInterzisException(final String message) {
        super(message);
    }
}