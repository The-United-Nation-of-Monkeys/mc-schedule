package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Расписание")
public class Schedule {

    private Integer allPairCount;

    private List<Day> days;
}
