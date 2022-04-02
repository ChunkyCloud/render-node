package de.lemaik.renderservice.renderer.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class Message {
    private static final Gson GSON = new Gson();

    private ErrorMessage Error;
    private WarningMessage Warning;
    private ServerInfoMessage ServerInfo;
    private Object[] AuthenticationRequest;
    private AuthenticationMessage Authentication;
    private Object[] AuthenticationOk;
    private Object[] TaskGet;
    private TaskMessage Task;
    private Object[] TaskComplete;

    public Message() {
        this.Error = null;
        this.Warning = null;
        this.AuthenticationRequest = null;
        this.Authentication = null;
        this.TaskGet = null;
        this.Task = null;
    }

    public static Message parse(String message) throws JsonSyntaxException {
        return GSON.fromJson(message, Message.class);
    }

    public String toJson() {
        return toJson(GSON);
    }

    private String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return toJson(new GsonBuilder().setPrettyPrinting().create());
    }

    public static Message empty() {
        return new Message();
    }

    public static Message error(String message) {
        Message m = new Message();
        m.Error = new ErrorMessage();
        m.Error.message = message;
        return m;
    }

    public ErrorMessage getError() {
        return Error;
    }

    public static Message warning(String message) {
        Message m = new Message();
        m.Warning = new WarningMessage();
        m.Warning.message = message;
        return m;
    }

    public WarningMessage getWarning() {
        return Warning;
    }

    public static Message authenticationRequest() {
        Message m = new Message();
        m.AuthenticationRequest = new Object[0];
        return m;
    }

    public ServerInfoMessage getServerInfo() {
        return ServerInfo;
    }

    public boolean getAuthenticationRequest() {
        return AuthenticationRequest != null;
    }

    public static Message authentication(String token) {
        Message m = new Message();
        m.Authentication = new AuthenticationMessage();
        m.Authentication.token = token;
        return m;
    }

    public AuthenticationMessage getAuthentication() {
        return Authentication;
    }

    public boolean getAuthenticationOk() {
        return AuthenticationOk != null;
    }

    public static Message taskGet() {
        Message m = new Message();
        m.TaskGet = new Object[0];
        return m;
    }

    public boolean getTaskGet() {
        return TaskGet != null;
    }

    public static Message task(String jobId, int spp) {
        Message m = new Message();
        m.Task = new TaskMessage();
        m.Task.job_id = jobId;
        m.Task.spp = spp;
        return m;
    }

    public TaskMessage getTask() {
        return Task;
    }

    public static Message taskComplete() {
        Message m = new Message();
        m.TaskComplete = new Object[0];
        return m;
    }

    public boolean getTaskComplete() {
        return TaskComplete != null;
    }
}
