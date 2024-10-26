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

    private Integer year;

    private String dayWeek;

    private String fullDayName;

    @Schema(description = "количество пар", example = "5")
    private Integer pairCount;

    private List<Pair> pairs;

    private List<List<Pair>> pairList;
}
