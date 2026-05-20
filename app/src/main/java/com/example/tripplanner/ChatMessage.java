package com.example.tripplanner;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    public int type;
    public String message;
    public long timestamp;

    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
