package com.project.ScheduleParsing.dto;

import com.project.ScheduleParsing.annotation.ExcludeFromGson;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Учитель")
public class Teacher {

    @Schema(description = "id учителя", example = "3d17ec02-b644-4f64-83bb-6b8fddf9e2f4")
    private String id;

    @Schema(description = "Фамилия", example = "Зернов")
    private String last_name;

    @Schema(description = "Первая буква иммени", example = "Г")
    private String first_name_one;

    @Schema(description = "Имя", example = "Глеб")
    private String first_name;

    @Schema(description = "Отчество", example = "Александрович")
    private String middle_name;

    @Schema(description = "Первая буква Отчества", example = "А")
    private String middle_name_one;

    @Schema(description = "ФИО", example = "Зернов Глеб Александрович")
    private String fio;

    @Schema(description = "Департамент и ФИО", example = "Зернов Глеб Александрович (Колледж Автономной некоммерческой образовательной организации высшего образования «Научно-технологический университет «Сириус»)")
    private String department_fio;

    @Schema(description = "Департамент", example = "Колледж Автономной некоммерческой образовательной организации высшего образования «Научно-технологический университет «Сириус»")
    private String department;

    @Schema(description = "Ссылка на фото")
    @ExcludeFromGson
    private String photoUrl;
}
