package com.zjgsu.czh.courseservice.repository;

import com.zjgsu.czh.courseservice.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 只有课程仍有剩余名额时才原子地增加预约人数。
     * 返回 1 表示更新成功，返回 0 表示课程不存在或名额已满。
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Course c SET c.bookedCount = c.bookedCount + 1 "
            + "WHERE c.id = :id AND c.bookedCount < c.capacity")
    int bookIfAvailable(@Param("id") Long id);
}
