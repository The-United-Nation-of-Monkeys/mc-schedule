package com.project.ScheduleParsing.controller;

import com.project.ScheduleParsing.dto.ClassroomListResponse;
import com.project.ScheduleParsing.dto.GroupListResponse;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.dto.TeachersListResponse;
import com.project.ScheduleParsing.service.ClassroomScheduleService;
import com.project.ScheduleParsing.service.GroupScheduleService;
import com.project.ScheduleParsing.service.TeacherScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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

    @GetMapping("/teacher")
    public ResponseEntity<Schedule> getScheduleByTeacher(@RequestParam String teacher, @RequestParam int week) {
        return ResponseEntity.ok(teacherScheduleService.getScheduleByTeacher(teacher, week));
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
    public ResponseEntity<TeachersListResponse> getTeachers(@RequestParam String search) {
        return ResponseEntity.ok(teacherScheduleService.getTeachers(search));
    }

    @GetMapping("/classrooms")
    public ResponseEntity<ClassroomListResponse> getClassrooms(@RequestParam String search) {
        return ResponseEntity.ok(classroomScheduleService.getClassrooms(search));
    }
}
