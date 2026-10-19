package com.zjgsu.czh.monolith.service;

import com.zjgsu.czh.monolith.model.Course;
import com.zjgsu.czh.monolith.model.Member;
import com.zjgsu.czh.monolith.repository.CourseRepository;
import com.zjgsu.czh.monolith.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Transactional
    public String bookCourse(Long memberId, Long courseId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));


        if (member.getRemainingSessions() <= 0) {
            throw new IllegalStateException("预约失败：会员剩余课时不足");
        }
        if (course.getBookedCount() >= course.getCapacity()) {
            throw new IllegalStateException("预约失败：课程名额已满");
        }


        member.setRemainingSessions(member.getRemainingSessions() - 1);
        course.setBookedCount(course.getBookedCount() + 1);


        memberRepository.save(member);
        courseRepository.save(course);

        return "预约成功！课程：" + course.getName() + "，剩余课时：" + member.getRemainingSessions();
    }
}