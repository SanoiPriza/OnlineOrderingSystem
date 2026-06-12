package org.example.productService.controller.admin;

import org.example.productService.dto.admin.DlqMessageInfoDto;
import org.example.productService.dto.admin.DlqQueueInfoDto;
import org.example.productService.dto.admin.DlqRetryResultDto;
import org.example.productService.service.DlqAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class DlqAdminController {

    private static final Logger log = LoggerFactory.getLogger(DlqAdminController.class);

    private final DlqAdminService dlqAdminService;
    private final String adminToken;

    public DlqAdminController(DlqAdminService dlqAdminService,
                               @Value("${internal.service.token}") String adminToken) {
        this.dlqAdminService = dlqAdminService;
        this.adminToken = adminToken;
    }

    @GetMapping("/dlq")
    public List<DlqQueueInfoDto> listDlqs(@RequestHeader("X-Admin-Token") String token) {
        requireAdminToken(token);
        return dlqAdminService.getAllDlqInfo();
    }

    @GetMapping("/dlq/{queueName}/messages")
    public List<DlqMessageInfoDto> peekMessages(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String queueName,
            @RequestParam(defaultValue = "20") int limit) {
        requireAdminToken(token);
        return dlqAdminService.peekMessages(queueName, Math.min(limit, 100));
    }

    @PostMapping("/dlq/{queueName}/retry-all")
    public DlqRetryResultDto retryAll(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String queueName) {
        requireAdminToken(token);
        log.info("Admin DLQ retry-all triggered for queue '{}'.", queueName);
        return dlqAdminService.retryAll(queueName);
    }

    private void requireAdminToken(String provided) {
        if (!adminToken.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin token");
        }
    }
}
