package com.zjgsu.czh.monolith.controller;

import com.zjgsu.czh.monolith.dto.BookingRequest;
import com.zjgsu.czh.monolith.model.Course;
import com.zjgsu.czh.monolith.model.Member;
import com.zjgsu.czh.monolith.repository.CourseRepository;
import com.zjgsu.czh.monolith.repository.MemberRepository;
import com.zjgsu.czh.monolith.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CourseRepository courseRepository;


    @PostMapping("/member")
    public Member createMember(@RequestBody Member member) {
        return memberRepository.save(member);
    }


    @PostMapping("/course")
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }


    @GetMapping("/courses")
    public List<Course> listCourses() {
        return courseRepository.findAll();
    }


    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestBody BookingRequest request) {
        try {
            String result = bookingService.bookCourse(request.getMemberId(), request.getCourseId());
            return ResponseEntity.ok(Map.of("code", 200, "message", result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", 400, "message", e.getMessage()));
        }
    }
}