package com.project.ScheduleParsing.request.servermemo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataForAuditory {

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
    private String classroom;
    private List<String> classroomsList;
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
