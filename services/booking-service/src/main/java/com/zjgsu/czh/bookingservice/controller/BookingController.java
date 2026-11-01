package com.zjgsu.czh.bookingservice.controller;

import com.zjgsu.czh.bookingservice.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @PostMapping
    public String book(@RequestParam Long memberId, @RequestParam Long courseId) {
        return bookingService.bookCourse(memberId, courseId);
    }
}