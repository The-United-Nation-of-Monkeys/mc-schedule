package com.project.ScheduleParsing.dto;

import com.project.ScheduleParsing.annotation.ExcludeFromGson;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Пара")
public class Pair {

    @Schema(description = "Дата", example = "5.10.2024")
    private String date;

    @Schema(description = "День недели", example = "ПН")
    private String dayWeek;

    @Schema(description = "Время начала пары", example = "09:00")
    private String startTime;

    @Schema(description = "Время конца пары", example = "10:30")
    private String endTime;

    @Schema(description = "Дисциплина", example = "Основы программирования")
    private String discipline;

    @Schema(description = "Тип занятия", example = "Практические занятия")
    private String groupType;

    @Schema(description = "Адрес", example = "Основной")
    private String address;

    @Schema(description = "Аудитория", example = "К_7")
    private String classroom;

    @Schema(description = "Комментарий", example = "Хз, не опаздывать")
    private String comment;

    @Schema(description = "Место", example = "Тоже хз, что-то в лицее")
    private String place;

    @Schema(description = "Список учителей", implementation = Teacher.class)
    @ExcludeFromGson
    private List<Object> teachers;

    @Schema(description = "Ссылка на онлайн занятие", example = "https://example.com/")
    private String urlOnline;

    @Schema(description = "Группа", example = "К0709-23/3")
    private String group;

    @Schema(description = "Номер пары", example = "1")
    private Integer numberPair;
}
