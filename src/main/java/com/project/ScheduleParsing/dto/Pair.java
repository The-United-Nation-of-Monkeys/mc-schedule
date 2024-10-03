package com.project.ScheduleParsing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pair {

    private String pairNumber;

    private String pairType;

    private String pairName;

    private String pairTeacher;

    private String pairLocation;

    private String pairTime;
}
