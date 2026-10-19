package com.zjgsu.czh.monolith;

import com.zjgsu.czh.monolith.model.Course;
import com.zjgsu.czh.monolith.model.Member;
import com.zjgsu.czh.monolith.repository.CourseRepository;
import com.zjgsu.czh.monolith.repository.MemberRepository;
import com.zjgsu.czh.monolith.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingServiceTest {

    @Autowired private BookingService bookingService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CourseRepository courseRepository;

    @Test
    void testBookCourseSuccess() {

        Member member = memberRepository.save(new Member("测试用户", "13800000000", 5));
        Course course = courseRepository.save(new Course("测试瑜伽", "王教练", 10, 0));


        String result = bookingService.bookCourse(member.getId(), course.getId());


        assertTrue(result.contains("预约成功"));
        assertEquals(4, memberRepository.findById(member.getId()).get().getRemainingSessions());
        assertEquals(1, courseRepository.findById(course.getId()).get().getBookedCount());
    }

    @Test
    void testBookCourseFailDueToNoSessions() {

        Member member = memberRepository.save(new Member("穷光蛋", "13900000000", 0));
        Course course = courseRepository.save(new Course("高级单车", "赵教练", 10, 0));


        Exception exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.bookCourse(member.getId(), course.getId());
        });
        assertTrue(exception.getMessage().contains("课时不足"));
    }
}