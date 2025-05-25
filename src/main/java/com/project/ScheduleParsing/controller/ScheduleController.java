package com.project.ScheduleParsing.controller;

import com.project.ScheduleParsing.dto.*;
import com.project.ScheduleParsing.service.ClassroomScheduleService;
import com.project.ScheduleParsing.service.GroupScheduleService;
import com.project.ScheduleParsing.service.TeacherScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final GroupScheduleService groupScheduleService;

    private final TeacherScheduleService teacherScheduleService;

    private final ClassroomScheduleService classroomScheduleService;

    @GetMapping("/group")
    public ResponseEntity<Schedule> getScheduleByGroup(@RequestParam String group, @RequestParam int week) {
        return ResponseEntity.ok(groupScheduleService.getScheduleByGroup(group, week));
    }

    @GetMapping("/group/now")
    public ResponseEntity<List<Pair>> getScheduleByGroupNow(@RequestParam String group) {
        return ResponseEntity.ok(groupScheduleService.getScheduleByGroupNow(group));
    }

    @GetMapping("/teacher")
    public ResponseEntity<Schedule> getScheduleByTeacher(@RequestParam String teacher, @RequestParam int week) {
        TeachersListResponse teacherResponse = teacherScheduleService.getTeachers(teacher);
        Teacher teacherDto = (Teacher) teacherResponse.getTeachers().get(0);
        return ResponseEntity.ok(teacherScheduleService.getScheduleByTeacher(teacherDto.getId(), week));
    }

    @GetMapping("/classroom")
    public ResponseEntity<Schedule> getScheduleByAuditory(@RequestParam String classroom, @RequestParam int week) {
        return ResponseEntity.ok(classroomScheduleService.getScheduleByClassroom(classroom, week));
    }

    @GetMapping("/groups")
    public ResponseEntity<GroupListResponse> getGroups(@RequestParam String search) {
        return ResponseEntity.ok(groupScheduleService.getGroups(search));
    }

    @GetMapping("/teachers")
    public TeachersListResponse getTeachers(@RequestParam String search) {
        TeachersListResponse response = teacherScheduleService.getTeachers(search);
        List<Object> teachers = new ArrayList<>();
        for (Object teacher : response.getTeachers()) {
            try {
                teachers.add(((Teacher) teacher).getFio());
            } catch (Exception e) {
                teachers.add(teacher);
            }
        }
        return new TeachersListResponse(teachers.size(), teachers);
    }

    @GetMapping("/classrooms")
    public ResponseEntity<ClassroomListResponse> getClassrooms(@RequestParam String search) {
        return ResponseEntity.ok(classroomScheduleService.getClassrooms(search));
    }


}
