package com.zjgsu.czh.userservice.controller;

import com.zjgsu.czh.userservice.model.Member;
import com.zjgsu.czh.userservice.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    @Autowired
    private MemberRepository memberRepository;

    @PostMapping
    public Member addMember(@RequestBody Member member) {
        return memberRepository.save(member);
    }

    @GetMapping("/{id}/check")
    public String checkMember(@PathVariable Long id) {
        Optional<Member> member = memberRepository.findById(id);
        if (member.isEmpty()) return "NOT_FOUND";
        if (member.get().getRemainingSessions() <= 0) return "NO_SESSIONS";
        return "OK";
    }
    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}