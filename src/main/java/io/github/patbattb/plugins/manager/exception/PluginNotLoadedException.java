package io.github.patbattb.plugins.manager.exception;

public class PluginNotLoadedException extends Exception {
    public PluginNotLoadedException() {
    }

    public PluginNotLoadedException(String message) {
        super(message);
    }

    public PluginNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginNotLoadedException(Throwable cause) {
        super(cause);
    }
}
