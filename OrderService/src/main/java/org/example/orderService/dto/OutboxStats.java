package org.example.orderService.dto;

public class OutboxStats {
    private long pending;
    private long processing;
    private long completed;
    private long failed;
    private long stuckProcessing;

    public OutboxStats() {}

    public OutboxStats(long pending, long processing, long completed,
                       long failed, long stuckProcessing) {
        this.pending        = pending;
        this.processing     = processing;
        this.completed      = completed;
        this.failed         = failed;
        this.stuckProcessing = stuckProcessing;
    }

    public long getPending()         { return pending; }
    public long getProcessing()      { return processing; }
    public long getCompleted()       { return completed; }
    public long getFailed()          { return failed; }
    public long getStuckProcessing() { return stuckProcessing; }

    public void setPending(long v)         { pending = v; }
    public void setProcessing(long v)      { processing = v; }
    public void setCompleted(long v)       { completed = v; }
    public void setFailed(long v)          { failed = v; }
    public void setStuckProcessing(long v) { stuckProcessing = v; }
}
