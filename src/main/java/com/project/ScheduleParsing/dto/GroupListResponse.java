package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Список групп")
public class GroupListResponse {

    @Schema(description = "Количество групп")
    private Integer groupsCount;

    @Schema(description = "Список групп")
    private List<String> groups;
}
