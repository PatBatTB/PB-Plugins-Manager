package io.github.patbattb.plugins.manager.exception;

public class PluginNotFoundException extends Exception {
    public PluginNotFoundException() {
    }

    public PluginNotFoundException(String message) {
        super(message);
    }

    public PluginNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginNotFoundException(Throwable cause) {
        super(cause);
    }
}
