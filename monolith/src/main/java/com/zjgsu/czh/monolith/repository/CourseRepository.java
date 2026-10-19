package com.zjgsu.czh.monolith.repository;

import com.zjgsu.czh.monolith.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
}