package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Список учителей")
public class TeachersListResponse {

    @Schema(description = "Количество учителей", example = "10")
    private Integer teachersCount;

    @Schema(description = "Список учителей")
    private List<Object> teachers;
}
