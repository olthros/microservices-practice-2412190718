package com.zjgsu.czh.bookingservice.service;

import com.zjgsu.czh.bookingservice.client.CourseServiceClient;
import com.zjgsu.czh.bookingservice.client.CourseServiceClient.CourseBookResponse;
import com.zjgsu.czh.bookingservice.client.CourseServiceClient.CourseCheckResponse;
import com.zjgsu.czh.bookingservice.client.UserServiceClient;
import com.zjgsu.czh.bookingservice.client.UserServiceClient.MemberCheckResponse;
import com.zjgsu.czh.bookingservice.dto.BookingResponse;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final UserServiceClient userServiceClient;
    private final CourseServiceClient courseServiceClient;

    public BookingService(
            UserServiceClient userServiceClient,
            CourseServiceClient courseServiceClient) {
        this.userServiceClient = userServiceClient;
        this.courseServiceClient = courseServiceClient;
    }

    public BookingResponse bookCourse(Long memberId, Long courseId) {
        MemberCheckResponse member = callMemberCheck(memberId);
        validateMember(memberId, member);

        CourseCheckResponse course = callCourseCheck(courseId);
        validateCourse(courseId, course);

        CourseBookResponse booked = callCourseBooking(courseId);
        validateBookingResult(courseId, booked);

        return new BookingResponse(
                "SUCCESS",
                memberId,
                courseId,
                course.courseName(),
                booked.bookedCount(),
                member.instanceId(),
                course.instanceId(),
                booked.instanceId(),
                "预约成功");
    }

    private MemberCheckResponse callMemberCheck(Long memberId) {
        try {
            return userServiceClient.checkMember(memberId);
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "user-service 调用失败，预约未执行，请稍后重试",
                    exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "user-service 暂无可用实例，预约未执行，请稍后重试",
                    exception);
        }
    }

    private CourseCheckResponse callCourseCheck(Long courseId) {
        try {
            return courseServiceClient.checkCourse(courseId);
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "course-service 查询失败，预约未执行，请稍后重试",
                    exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "course-service 暂无可用实例，预约未执行，请稍后重试",
                    exception);
        }
    }

    private CourseBookResponse callCourseBooking(Long courseId) {
        try {
            return courseServiceClient.bookCourse(courseId);
        } catch (RetryableException exception) {
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "WRITE_RESULT_UNKNOWN：课程预约写操作超时，结果未知；"
                            + "请先查询课程状态，不要直接重复提交",
                    exception);
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "course-service 写调用失败；如果请求可能已到达，"
                            + "预约结果未知，请先查询课程状态",
                    exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "course-service 写调用异常，预约结果未知，"
                            + "请先查询课程状态，不要直接重复提交",
                    exception);
        }
    }

    private void validateMember(
            Long memberId,
            MemberCheckResponse member) {

        if (member == null || member.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "user-service 返回了无法识别的空响应");
        }

        if ("NOT_FOUND".equals(member.status())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "会员不存在，memberId=" + memberId);
        }

        if ("NO_SESSIONS".equals(member.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "会员剩余课时不足，memberId=" + memberId);
        }

        if (!"OK".equals(member.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "user-service 返回未知状态：" + member.status());
        }
    }

    private void validateCourse(
            Long courseId,
            CourseCheckResponse course) {

        if (course == null || course.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "course-service 返回了无法识别的空响应");
        }

        if ("NOT_FOUND".equals(course.status())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "课程不存在，courseId=" + courseId);
        }

        if ("FULL".equals(course.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "课程名额已满，courseId=" + courseId);
        }

        if (!"OK".equals(course.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "course-service 返回未知状态：" + course.status());
        }
    }

    private void validateBookingResult(
            Long courseId,
            CourseBookResponse booked) {

        if (booked == null || booked.status() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "course-service 返回了无法识别的预约结果");
        }

        if ("NOT_FOUND".equals(booked.status())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "预约时课程已不存在，courseId=" + courseId);
        }

        if ("FULL".equals(booked.status())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "预约时课程名额已满，courseId=" + courseId);
        }

        if (!"SUCCESS".equals(booked.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "course-service 返回未知预约状态：" + booked.status());
        }
    }
}
