package com.zjgsu.czh.bookingservice.controller;

import com.zjgsu.czh.bookingservice.dto.BookingResponse;
import com.zjgsu.czh.bookingservice.service.BookingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse book(@RequestParam Long memberId,
                                @RequestParam Long courseId) {
        return bookingService.bookCourse(memberId, courseId);
    }
}
