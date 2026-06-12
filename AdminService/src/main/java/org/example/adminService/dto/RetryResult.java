package org.example.adminService.dto;

public class RetryResult {
    private String queue;
    private int    requeued;
    private String message;

    public RetryResult() {}
    public RetryResult(String queue, int requeued, String message) {
        this.queue = queue; this.requeued = requeued; this.message = message;
    }

    public String getQueue()   { return queue; }
    public int    getRequeued(){ return requeued; }
    public String getMessage() { return message; }

    public void setQueue(String v)   { this.queue = v; }
    public void setRequeued(int v)   { this.requeued = v; }
    public void setMessage(String v) { this.message = v; }
}
