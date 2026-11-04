package com.collabsphere_first.profile.exception;

public class ProfileAlreadyCompletedException extends RuntimeException {

    public ProfileAlreadyCompletedException() {
        super("Profile has already been completed.");
    }

    public ProfileAlreadyCompletedException(String message) {
        super(message);
    }
}
