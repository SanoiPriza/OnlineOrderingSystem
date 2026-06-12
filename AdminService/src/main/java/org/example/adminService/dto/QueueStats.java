package org.example.adminService.dto;

public class QueueStats {
    private String name;
    private long   messages;
    private long   messagesReady;
    private long   messagesUnacknowledged;
    private String state;
    private boolean dlq;

    public QueueStats() {}
    public QueueStats(String name, long messages, long messagesReady,
                      long messagesUnacknowledged, String state, boolean dlq) {
        this.name = name; this.messages = messages;
        this.messagesReady = messagesReady;
        this.messagesUnacknowledged = messagesUnacknowledged;
        this.state = state; this.dlq = dlq;
    }

    public String  getName()                 { return name; }
    public long    getMessages()             { return messages; }
    public long    getMessagesReady()        { return messagesReady; }
    public long    getMessagesUnacknowledged(){ return messagesUnacknowledged; }
    public String  getState()               { return state; }
    public boolean isDlq()                  { return dlq; }

    public void setName(String v)                   { this.name = v; }
    public void setMessages(long v)                 { this.messages = v; }
    public void setMessagesReady(long v)            { this.messagesReady = v; }
    public void setMessagesUnacknowledged(long v)   { this.messagesUnacknowledged = v; }
    public void setState(String v)                  { this.state = v; }
    public void setDlq(boolean v)                   { this.dlq = v; }
}
