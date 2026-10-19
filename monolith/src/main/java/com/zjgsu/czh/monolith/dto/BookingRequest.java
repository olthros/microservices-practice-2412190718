package com.zjgsu.czh.monolith.dto;

public class BookingRequest {
    private Long memberId;
    private Long courseId;


    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
}