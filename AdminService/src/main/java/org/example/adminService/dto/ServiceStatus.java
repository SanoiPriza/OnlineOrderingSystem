package org.example.adminService.dto;

public class ServiceStatus {
    private String name;
    private String url;
    private String status;
    private String details;

    public ServiceStatus() {}
    public ServiceStatus(String name, String url, String status, String details) {
        this.name = name; this.url = url; this.status = status; this.details = details;
    }

    public String getName()    { return name; }
    public String getUrl()     { return url; }
    public String getStatus()  { return status; }
    public String getDetails() { return details; }

    public void setName(String v)    { this.name = v; }
    public void setUrl(String v)     { this.url = v; }
    public void setStatus(String v)  { this.status = v; }
    public void setDetails(String v) { this.details = v; }
}
