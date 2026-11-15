package com.zjgsu.czh.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/api/members")
public interface UserServiceClient {

    @GetMapping("/{memberId}/check")
    MemberCheckResponse checkMember(
            @PathVariable("memberId") Long memberId);

    record MemberCheckResponse(
            String status,
            Long memberId,
            String memberName,
            Integer remainingSessions,
            String instanceId) {
    }
}
