package com.zjgsu.czh.courseservice.controller;

import com.zjgsu.czh.courseservice.model.Course;
import com.zjgsu.czh.courseservice.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Autowired
    private CourseRepository courseRepository;

    @PostMapping
    public Course addCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    @GetMapping("/{id}/check")
    public String checkCourse(@PathVariable Long id) {
        Optional<Course> course = courseRepository.findById(id);
        if (course.isEmpty()) return "NOT_FOUND";
        if (course.get().getBookedCount() >= course.get().getCapacity()) return "FULL";
        return "OK";
    }

    @PostMapping("/{id}/book")
    public String bookCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setBookedCount(course.getBookedCount() + 1);
        courseRepository.save(course);
        return "SUCCESS";
    }
}