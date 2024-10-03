package com.project.ScheduleParsing.request.servermemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Data {

    private String year;
    private String date;
    private Month month;
    private int numWeek;
    private int addNumWeek;
    private int minusNumWeek;
    private int count;
    private String type;
    private String search;
    private String group;
    private List<Object> groupList;
    private List<Object> events;
    private List<Object> eventElement;
    private boolean watching;
    private String currentRouteName;
    private boolean statusInit;
    private boolean lectures;
    private boolean seminars;
    private boolean practices;
    private boolean laboratories;
    private boolean exams;
    private boolean other;
    private Integer width; // Changed to Integer to allow null
    private Integer height;
}
