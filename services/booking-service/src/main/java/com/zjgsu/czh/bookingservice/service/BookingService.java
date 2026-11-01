package com.zjgsu.czh.bookingservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BookingService {
    @Autowired
    private RestTemplate restTemplate;

    public String bookCourse(Long memberId, Long courseId) {
        String userUrl = "http://user-service:8081/api/members/" + memberId + "/check";
        String courseCheckUrl = "http://course-service:8082/api/courses/" + courseId + "/check";
        String courseBookUrl = "http://course-service:8082/api/courses/" + courseId + "/book";

        try {
            String userCheck = restTemplate.getForObject(userUrl, String.class);
            if (!"OK".equals(userCheck)) {
                return "FAILED: user check failed, status=" + userCheck;
            }

            String courseCheck = restTemplate.getForObject(courseCheckUrl, String.class);
            if (!"OK".equals(courseCheck)) {
                return "FAILED: course check failed, status=" + courseCheck;
            }

            restTemplate.postForObject(courseBookUrl, null, String.class);
            return "SUCCESS";
        } catch (Exception e) {
            return "FAILED: remote service unavailable. " + e.getMessage();
        }
    }
}