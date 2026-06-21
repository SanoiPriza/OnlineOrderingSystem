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

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Service-Token";

    private final String baseUrl;
    private final HttpClient http;
    private final String internalToken;
    private final ObjectMapper mapper;

    public AdminApiClient(String baseUrl, String internalToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.internalToken = internalToken;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public DashboardSnapshot fetchSnapshot() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/snapshot"))
                .header(HEADER_INTERNAL_TOKEN, internalToken)
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
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/dlq/" + queueName + "/retry"))
                .header(HEADER_INTERNAL_TOKEN, internalToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " : " + resp.body());
        }
        return mapper.readValue(resp.body(), RetryResult.class);
    }
}
