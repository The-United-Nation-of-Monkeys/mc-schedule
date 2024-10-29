package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "День недели")
public class Day {

    @Schema(description = "День", example = "5")
    private Integer day;

    @Schema(description = "месяц", example = "10")
    private Integer month;

    @Schema(description = "год", example = "2024")
    private Integer year;

    @Schema(description = "День недели", example = "ПН")
    private String dayWeek;

    @Schema(description = "месяц", example = "Понедельник, 5 Октября")
    private String fullDayName;

    @Schema(description = "количество пар", example = "25")
    private Integer pairCount;

    @Schema(description = "Список пар")
    private List<List<Pair>> pairList;
}
