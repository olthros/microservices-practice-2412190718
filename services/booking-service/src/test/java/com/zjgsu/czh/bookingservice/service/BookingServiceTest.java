package com.zjgsu.czh.bookingservice.service;

import com.zjgsu.czh.bookingservice.client.CourseServiceClient;
import com.zjgsu.czh.bookingservice.client.CourseServiceClient.CourseBookResponse;
import com.zjgsu.czh.bookingservice.client.CourseServiceClient.CourseCheckResponse;
import com.zjgsu.czh.bookingservice.client.UserServiceClient;
import com.zjgsu.czh.bookingservice.client.UserServiceClient.MemberCheckResponse;
import com.zjgsu.czh.bookingservice.dto.BookingResponse;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CourseServiceClient courseServiceClient;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void bookCourse_shouldReturnSuccessAndInstanceIds() {
        mockSuccessfulChecks();
        when(courseServiceClient.bookCourse(1L)).thenReturn(
                new CourseBookResponse(
                        "SUCCESS",
                        1L,
                        6,
                        "course-1:8082"));

        BookingResponse response =
                bookingService.bookCourse(1L, 1L);

        assertEquals("SUCCESS", response.status());
        assertEquals(6, response.bookedCount());
        assertEquals(
                "user-1:8081",
                response.memberInstanceId());
        assertEquals(
                "course-1:8082",
                response.courseCheckInstanceId());
        assertEquals(
                "course-1:8082",
                response.courseInstanceId());

        verify(courseServiceClient).bookCourse(1L);
    }

    @Test
    void bookCourse_shouldRejectMemberWithoutSessions() {
        when(userServiceClient.checkMember(1L)).thenReturn(
                new MemberCheckResponse(
                        "NO_SESSIONS",
                        1L,
                        "测试会员",
                        0,
                        "user-1:8081"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.bookCourse(1L, 1L));

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(courseServiceClient);
    }

    @Test
    void bookCourse_shouldRejectFullCourseWithoutWriting() {
        when(userServiceClient.checkMember(1L)).thenReturn(
                new MemberCheckResponse(
                        "OK",
                        1L,
                        "测试会员",
                        10,
                        "user-1:8081"));

        when(courseServiceClient.checkCourse(1L)).thenReturn(
                new CourseCheckResponse(
                        "FULL",
                        1L,
                        "瑜伽",
                        20,
                        20,
                        "course-1:8082"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.bookCourse(1L, 1L));

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode());

        verify(courseServiceClient, never())
                .bookCourse(anyLong());
    }

    @Test
    void bookCourse_shouldReturn503WhenUserServiceIsUnavailable() {
        when(userServiceClient.checkMember(1L))
                .thenThrow(mock(FeignException.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.bookCourse(1L, 1L));

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode());

        assertTrue(exception.getReason().contains("预约未执行"));
        verifyNoInteractions(courseServiceClient);
    }

    @Test
    void bookCourse_shouldReturn504AndUnknownResultWhenWriteTimesOut() {
        mockSuccessfulChecks();

        when(courseServiceClient.bookCourse(1L))
                .thenThrow(mock(RetryableException.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.bookCourse(1L, 1L));

        assertEquals(
                HttpStatus.GATEWAY_TIMEOUT,
                exception.getStatusCode());

        assertTrue(
                exception.getReason().contains("WRITE_RESULT_UNKNOWN"));
    }

    private void mockSuccessfulChecks() {
        when(userServiceClient.checkMember(1L)).thenReturn(
                new MemberCheckResponse(
                        "OK",
                        1L,
                        "测试会员",
                        10,
                        "user-1:8081"));

        when(courseServiceClient.checkCourse(1L)).thenReturn(
                new CourseCheckResponse(
                        "OK",
                        1L,
                        "瑜伽",
                        20,
                        5,
                        "course-1:8082"));
    }
}
