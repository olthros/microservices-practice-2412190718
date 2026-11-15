package com.zjgsu.czh.courseservice.controller;

import com.zjgsu.czh.courseservice.model.Course;
import com.zjgsu.czh.courseservice.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;
    private final String instanceId;

    public CourseController(
            CourseRepository courseRepository,
            @Value("${HOSTNAME:local}") String hostname,
            @Value("${server.port:8082}") int serverPort) {
        this.courseRepository = courseRepository;
        this.instanceId = hostname + ":" + serverPort;
    }

    @PostMapping
    public Course addCourse(@RequestBody Course course) {
        if (course.getBookedCount() == null) {
            course.setBookedCount(0);
        }
        return courseRepository.save(course);
    }

    @GetMapping("/{id}/check")
    public CourseCheckResponse checkCourse(@PathVariable Long id) {
        Optional<Course> optionalCourse = courseRepository.findById(id);

        if (optionalCourse.isEmpty()) {
            return new CourseCheckResponse(
                    "NOT_FOUND",
                    id,
                    null,
                    null,
                    null,
                    instanceId);
        }

        Course course = optionalCourse.get();
        String status =
                course.getBookedCount() >= course.getCapacity()
                        ? "FULL"
                        : "OK";

        return new CourseCheckResponse(
                status,
                course.getId(),
                course.getName(),
                course.getCapacity(),
                course.getBookedCount(),
                instanceId);
    }

    @PostMapping("/{id}/book")
    @Transactional
    public CourseBookResponse bookCourse(@PathVariable Long id) {
        int updatedRows = courseRepository.bookIfAvailable(id);
        Optional<Course> optionalCourse = courseRepository.findById(id);

        if (optionalCourse.isEmpty()) {
            return new CourseBookResponse(
                    "NOT_FOUND",
                    id,
                    null,
                    instanceId);
        }

        Course course = optionalCourse.get();

        if (updatedRows == 0) {
            return new CourseBookResponse(
                    "FULL",
                    id,
                    course.getBookedCount(),
                    instanceId);
        }

        return new CourseBookResponse(
                "SUCCESS",
                id,
                course.getBookedCount(),
                instanceId);
    }

    public record CourseCheckResponse(
            String status,
            Long courseId,
            String courseName,
            Integer capacity,
            Integer bookedCount,
            String instanceId) {
    }

    public record CourseBookResponse(
            String status,
            Long courseId,
            Integer bookedCount,
            String instanceId) {
    }
}
