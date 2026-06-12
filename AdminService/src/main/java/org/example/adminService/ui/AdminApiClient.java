package org.example.adminService.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.adminService.dto.DashboardSnapshot;
import org.example.adminService.dto.RetryResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AdminApiClient {

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public AdminApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public DashboardSnapshot fetchSnapshot() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/snapshot"))
                .GET()
                .timeout(Duration.ofSeconds(8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("AdminService returned HTTP " + resp.statusCode());
        }
        return mapper.readValue(resp.body(), DashboardSnapshot.class);
    }

    public RetryResult retryDlq(String queueName) throws Exception {
        String encodedQueue = queueName.replace(".", "%2E");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/dlq/" + encodedQueue + "/retry"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(resp.body(), RetryResult.class);
    }
}
