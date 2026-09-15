package com.invoiceflow.auth;

import com.invoiceflow.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException(String email) {
        super(HttpStatus.CONFLICT, "An account with email '" + email + "' already exists");
    }
}
