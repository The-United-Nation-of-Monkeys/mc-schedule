package com.project.ScheduleParsing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Day {

    private String dayOfWeek;

    private Integer pairCount;

    private List<Pair> pairs;
}
