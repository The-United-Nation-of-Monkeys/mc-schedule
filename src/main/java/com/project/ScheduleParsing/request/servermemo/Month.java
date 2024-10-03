package com.project.ScheduleParsing.request.servermemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Month {

    private String number;
    private String full;
    private String fullForDisplay;
}
