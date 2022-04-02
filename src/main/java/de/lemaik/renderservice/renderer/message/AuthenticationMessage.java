package de.lemaik.renderservice.renderer.message;

public class AuthenticationMessage {
    protected String token;

    protected AuthenticationMessage() {
        this.token = null;
    }

    public String getToken() {
        return token;
    }
}
