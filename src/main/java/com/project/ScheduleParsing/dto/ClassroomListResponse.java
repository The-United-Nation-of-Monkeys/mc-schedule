package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Список аудиторий")
public class ClassroomListResponse {

    @Schema(description = "Количество аудиторий")
    private Integer count;

    @Schema(description = "Список аудиторий")
    private List<String> auditoryList;
}
