package com.project.ScheduleParsing.repository;

import com.project.ScheduleParsing.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Teacher findTeacherByFio(String fio);
}
