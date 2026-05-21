package com.example.tripplanner;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_BOT_ITINERARY = 2;

    public int type;
    public String message;
    public long timestamp;
    public boolean actionTaken = false;

    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
