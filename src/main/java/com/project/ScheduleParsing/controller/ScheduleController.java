package com.project.ScheduleParsing.controller;

import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.service.GroupScheduleService;
import com.project.ScheduleParsing.service.TeacherScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final GroupScheduleService groupScheduleService;

    private final TeacherScheduleService teacherScheduleService;

    @GetMapping("/group")
    public ResponseEntity<Schedule> getScheduleByGroup(@RequestParam String group, @RequestParam int week) throws IOException {
        return ResponseEntity.ok(groupScheduleService.getScheduleByGroup(group, week));
    }

    @GetMapping("/teacher")
    public ResponseEntity<Schedule> getScheduleByTeacher(@RequestParam String teacher, @RequestParam int week) {
        return ResponseEntity.ok(teacherScheduleService.getScheduleByTeacher(teacher, week));
    }
}
