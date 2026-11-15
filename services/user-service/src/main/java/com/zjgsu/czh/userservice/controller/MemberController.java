package com.zjgsu.czh.userservice.controller;

import com.zjgsu.czh.userservice.model.Member;
import com.zjgsu.czh.userservice.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository memberRepository;
    private final String instanceId;

    public MemberController(
            MemberRepository memberRepository,
            @Value("${HOSTNAME:local}") String hostname,
            @Value("${server.port:8081}") int serverPort) {
        this.memberRepository = memberRepository;
        this.instanceId = hostname + ":" + serverPort;
    }

    @PostMapping
    public Member addMember(@RequestBody Member member) {
        return memberRepository.save(member);
    }

    @GetMapping("/{id}/check")
    public MemberCheckResponse checkMember(@PathVariable Long id) {
        Optional<Member> optionalMember = memberRepository.findById(id);

        if (optionalMember.isEmpty()) {
            return new MemberCheckResponse(
                    "NOT_FOUND",
                    id,
                    null,
                    null,
                    instanceId);
        }

        Member member = optionalMember.get();
        String status = member.getRemainingSessions() > 0
                ? "OK"
                : "NO_SESSIONS";

        return new MemberCheckResponse(
                status,
                member.getId(),
                member.getName(),
                member.getRemainingSessions(),
                instanceId);
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }

    public record MemberCheckResponse(
            String status,
            Long memberId,
            String memberName,
            Integer remainingSessions,
            String instanceId) {
    }
}
