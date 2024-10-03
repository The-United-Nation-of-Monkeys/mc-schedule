package com.project.ScheduleParsing.controller;

import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.service.ScheduleParsingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleParsingService scheduleParsingService;

    @GetMapping("/group")
    public ResponseEntity<Schedule> getDialog(@RequestParam String group, @RequestParam int week) throws IOException {
        return ResponseEntity.ok(scheduleParsingService.getSchedule(group, week));
    }
}
