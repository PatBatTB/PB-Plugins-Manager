package io.github.patbattb.yougile.plugins.manager.exception;


public class PluginAlreadyRunException extends Exception {
    public PluginAlreadyRunException() {
    }

    public PluginAlreadyRunException(String message) {
        super(message);
    }

    public PluginAlreadyRunException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginAlreadyRunException(Throwable cause) {
        super(cause);
    }
}
