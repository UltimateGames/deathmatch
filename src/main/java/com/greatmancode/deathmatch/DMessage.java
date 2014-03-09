package com.greatmancode.deathmatch;

import me.ampayne2.ultimategames.api.message.Message;

public enum DMessage implements Message {
    KILL("Kill", "%s killed %s!"),
    DEATH("Death", "%s died!"),
    GAME_END("GameEnd", "%s won %s on arena %s!");

    private String message;
    private final String path;
    private final String defaultMessage;

    private DMessage(String path, String defaultMessage) {
        this.path = path;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDefault() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return message;
    }
}
