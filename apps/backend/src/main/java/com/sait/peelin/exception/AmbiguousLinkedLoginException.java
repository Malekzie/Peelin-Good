package com.sait.peelin.exception;

import com.sait.peelin.dto.v1.auth.LoginAccountChoice;

import java.util.List;

/**
 * Raised when the same email/username and password match two active users who share an employee↔customer link.
 * Clients should repeat login with {@code username} set to the chosen account.
 */
public class AmbiguousLinkedLoginException extends RuntimeException {

    private final List<LoginAccountChoice> choices;

    public AmbiguousLinkedLoginException(String message, List<LoginAccountChoice> choices) {
        super(message);
        this.choices = List.copyOf(choices);
    }

    public List<LoginAccountChoice> getChoices() {
        return choices;
    }
}
