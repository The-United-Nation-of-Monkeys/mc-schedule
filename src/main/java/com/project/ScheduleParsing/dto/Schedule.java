package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Расписание")
public class Schedule {

    @Schema(description = "Общее количество пар за неделю", example = "25")
    private Integer allPairCount;

    @Schema(description = "Список дней")
    private List<Day> days;
}
