package com.zjgsu.czh.gatewayservice.controller;

import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final RouteDefinitionLocator routeDefinitionLocator;

    public HealthController(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @GetMapping("/api/health")
    public Mono<Map<String, Object>> health() {
        return routeDefinitionLocator.getRouteDefinitions()
                .map(RouteDefinition::getId)
                .collectList()
                .map(routeIds -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("status", "UP");
                    body.put("service", "gateway-service");
                    body.put("timestamp", OffsetDateTime.now());
                    body.put("routes", routeIds);
                    return body;
                });
    }
}
