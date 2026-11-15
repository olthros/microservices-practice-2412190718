package com.zjgsu.czh.bookingservice.dto;

public record BookingResponse(
        String status,
        Long memberId,
        Long courseId,
        String courseName,
        Integer bookedCount,
        String memberInstanceId,
        String courseCheckInstanceId,
        String courseInstanceId,
        String message) {
}
