package com.project.ScheduleParsing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder

public class Schedule {

    private List<Day> days;
}
