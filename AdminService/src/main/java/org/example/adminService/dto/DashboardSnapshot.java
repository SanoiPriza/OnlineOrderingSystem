package org.example.adminService.dto;

import java.util.List;

public class DashboardSnapshot {
    private List<ServiceStatus> services;
    private List<QueueStats>    queues;
    private OutboxStats         outbox;
    private List<String>        knownDlqNames;
    private String              snapshotTime;

    public DashboardSnapshot() {}

    public List<ServiceStatus> getServices()    { return services; }
    public List<QueueStats>    getQueues()       { return queues; }
    public OutboxStats         getOutbox()       { return outbox; }
    public List<String>        getKnownDlqNames() { return knownDlqNames; }
    public String              getSnapshotTime() { return snapshotTime; }

    public void setServices(List<ServiceStatus> v)  { this.services = v; }
    public void setQueues(List<QueueStats> v)        { this.queues = v; }
    public void setOutbox(OutboxStats v)             { this.outbox = v; }
    public void setKnownDlqNames(List<String> v)     { this.knownDlqNames = v; }
    public void setSnapshotTime(String v)            { this.snapshotTime = v; }
}
