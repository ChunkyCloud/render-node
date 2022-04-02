package de.lemaik.renderservice.renderer.message;

public class ErrorMessage {
    protected String message;

    protected ErrorMessage() {
        this.message = null;
    }

    public String getMessage() {
        return message;
    }
}
