package com.zjgsu.czh.monolith.repository;

import com.zjgsu.czh.monolith.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}