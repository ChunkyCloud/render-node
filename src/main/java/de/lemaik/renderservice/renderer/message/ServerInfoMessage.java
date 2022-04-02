package de.lemaik.renderservice.renderer.message;

public class ServerInfoMessage {
    protected int protocol_version;

    protected ServerInfoMessage() {
        protocol_version = 0;
    }

    public int getProtocolVersion() {
        return this.protocol_version;
    }
}
