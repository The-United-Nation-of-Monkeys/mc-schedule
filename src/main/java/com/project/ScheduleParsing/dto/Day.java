package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "День недели")
public class Day {

    @Schema(description = "День", example = "5")
    private Integer day;

    @Schema(description = "месяц", example = "10")
    private Integer month;

//    @Schema(description = "Год", example = "2024")
//    private Integer year;

    @Schema(description = "количество пар", example = "5")
    private Integer pairCount;

    private List<Pair> pairs;
}
