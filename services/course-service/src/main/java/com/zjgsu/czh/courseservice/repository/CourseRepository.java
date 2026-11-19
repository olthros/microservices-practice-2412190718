package com.zjgsu.czh.courseservice.repository;

import com.zjgsu.czh.courseservice.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Course c SET c.bookedCount = c.bookedCount + 1 "
            + "WHERE c.id = :id AND c.bookedCount < c.capacity")
    int bookIfAvailable(@Param("id") Long id);
}
