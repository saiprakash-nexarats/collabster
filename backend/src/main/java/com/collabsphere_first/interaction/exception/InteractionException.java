package com.collabsphere_first.interaction.exception;

public abstract class InteractionException extends RuntimeException {
    public InteractionException(String message) {
        super(message);
    }

    public InteractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
