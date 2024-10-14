package com.project.ScheduleParsing.request.servermemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataForTeacher {

    private String year;
    private String date;
    private Month month;
    private int numWeek;
    private int addNumWeek;
    private int minusNumWeek;
    private int count;
    private String type;
    private String gridRoute;
    private String listRoute;
    private String search;
    private String teacherName;
    private String teacher;
    private boolean teacherShow;
    private Map<String, Object> teachersList;
    private String currentRouteName;
    private boolean watching;
    private List<Object> events;
    private List<Object> eventElement;
    private boolean statusInit;
    private boolean lectures;
    private boolean seminars;
    private boolean practices;
    private boolean laboratories;
    private boolean exams;
    private boolean other;
    private Integer width;
    private Integer height;
}
