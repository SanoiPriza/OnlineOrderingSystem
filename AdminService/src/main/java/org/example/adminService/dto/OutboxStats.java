package org.example.adminService.dto;

public class OutboxStats {
    private long pending;
    private long processing;
    private long completed;
    private long failed;

    public OutboxStats() {}
    public OutboxStats(long pending, long processing, long completed, long failed) {
        this.pending = pending; this.processing = processing;
        this.completed = completed; this.failed = failed;
    }

    public long getPending()    { return pending; }
    public long getProcessing() { return processing; }
    public long getCompleted()  { return completed; }
    public long getFailed()     { return failed; }

    public void setPending(long v)    { this.pending = v; }
    public void setProcessing(long v) { this.processing = v; }
    public void setCompleted(long v)  { this.completed = v; }
    public void setFailed(long v)     { this.failed = v; }
}
