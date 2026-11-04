package com.collabsphere_first.profile.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Username is already taken.");
    }

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}