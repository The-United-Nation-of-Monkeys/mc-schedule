package com.project.ScheduleParsing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Пара")
public class Pair {

    @Schema(description = "Номер пары", example = "3")
    private Integer pairNumber;

    @Schema(description = "Тип пары", example = "Практические занятия")
    private String pairType;

    @Schema(description = "Название пары", example = "Основы программирования")
    private String pairName;

    @Schema(description = "Преподаватель пары", example = "Зернов Г. А.")
    private String pairTeacher;

    @Schema(description = "Аудитория", example = "К_5")
    private String pairLocation;

    @Schema(description = "Время занятий", example = "13:15 - 14:45")
    private String pairTime;
}
