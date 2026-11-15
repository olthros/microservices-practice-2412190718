package com.zjgsu.czh.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "course-service", path = "/api/courses")
public interface CourseServiceClient {

    @GetMapping("/{courseId}/check")
    CourseCheckResponse checkCourse(
            @PathVariable("courseId") Long courseId);

    @PostMapping("/{courseId}/book")
    CourseBookResponse bookCourse(
            @PathVariable("courseId") Long courseId);

    record CourseCheckResponse(
            String status,
            Long courseId,
            String courseName,
            Integer capacity,
            Integer bookedCount,
            String instanceId) {
    }

    record CourseBookResponse(
            String status,
            Long courseId,
            Integer bookedCount,
            String instanceId) {
    }
}
