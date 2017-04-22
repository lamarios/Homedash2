package com.ftpix.homedash.plugins.dockercompose.exceptions;

/**
 * Created by gz on 4/22/17.
 */
public class CommandException extends Exception {
    private String message;

    public CommandException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


