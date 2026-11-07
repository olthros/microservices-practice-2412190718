package com.zjgsu.czh.bookingservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookingService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    public String bookCourse(Long memberId, Long courseId) {

        ServiceInstance userInstance = pickInstance("user-service");
        String userCheck = restTemplate.getForObject(
                userInstance.getUri() + "/api/members/" + memberId + "/check", String.class);
        if (!"OK".equals(userCheck)) {
            if ("NOT_FOUND".equals(userCheck)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found: " + memberId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user check failed: " + userCheck);
        }


        ServiceInstance courseInstance = pickInstance("course-service");
        String courseCheck = restTemplate.getForObject(
                courseInstance.getUri() + "/api/courses/" + courseId + "/check", String.class);
        if (!"OK".equals(courseCheck)) {
            if ("NOT_FOUND".equals(courseCheck)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "course not found: " + courseId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course check failed: " + courseCheck);
        }


        restTemplate.postForObject(
                courseInstance.getUri() + "/api/courses/" + courseId + "/book", null, String.class);
        return "SUCCESS";
    }


    private ServiceInstance pickInstance(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "no available instance of service: " + serviceName);
        }
        return instances.get(0);
    }
}
