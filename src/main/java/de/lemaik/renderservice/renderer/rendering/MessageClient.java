package de.lemaik.renderservice.renderer.rendering;

import com.google.gson.JsonSyntaxException;
import de.lemaik.renderservice.renderer.message.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageClient extends WebSocketClient {

    private static final Logger LOGGER = LogManager.getLogger(MessageClient.class);

    /**
     * We shouldn't ever have more then a handful of messages in waiting.
     */
    private static final int QUEUE_CAPACITY = 8;

    private final ArrayBlockingQueue<Message> messages = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    public interface Factory {
        MessageClient connect();
    }

    public MessageClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        LOGGER.info("Websocket client connected.");
    }

    @Override
    public void onMessage(String s) {
        LOGGER.debug("Received raw message: {}", s);

        Message message;
        try {
            message = Message.parse(s);
        } catch (JsonSyntaxException e) {
            LOGGER.info("Received invalid JSON: ", e);
            return;
        }

        // Error message
        if (message.getError() != null) {
            LOGGER.error("Received error message: {}", message.getError().getMessage());
            close();
            return;
        }

        // Warning message
        if (message.getWarning() != null) {
            LOGGER.warn("Received warning message: {}", message.getWarning().getMessage());
            return;
        }

        LOGGER.info("Received message: {}", message);
        if (!messages.offer(message)) {
            LOGGER.error("Message queue full.");
            close();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        LOGGER.info("Websocket client closed by {} with code {}: {}", b ? "remote": "us", i, s);
        this.messages.offer(Message.empty());  // Force any listeners to close
    }

    @Override
    public void onError(Exception e) {
        LOGGER.error("Encountered error in websocket client: ", e);
    }

    /**
     * Send a message.
     */
    public void send(Message message) {
        LOGGER.debug("Sent message: {}", message::toString);
        this.send(message.toJson());
    }

    /**
     * Wait for the next message.
     */
    public Message poll() throws InterruptedException {
        return this.messages.take();
    }

    /**
     * Wait for the next message.
     */
    public Message poll(long timeout, TimeUnit unit) throws InterruptedException {
        Message m = this.messages.poll(timeout, unit);
        if (m == null) {
            return Message.empty();
        }
        return m;
    }
}
