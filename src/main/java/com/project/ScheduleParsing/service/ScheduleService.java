package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.annotation.AnnotationExclusionStrategy;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.dto.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class ScheduleService {

    private final Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

    Schedule parseSchedule(String json) {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
        if (!data.has("events")) {
            return Schedule.builder()
                    .allPairCount(0)
                    .days(new ArrayList<>())
                    .build();
        }

        JSONObject events = data.getJSONObject("events");
        List<Day> days = new ArrayList<>(Collections.nCopies(7, null));
        int allPairCount = data.getInt("count");
        for (String key : events.keySet()) {
            Object eventObject = events.get(key);
            if (eventObject instanceof JSONObject event) {
                Day day = getDayFromJSONObject(event, key);
                days.set(getDayIndex(key.split(",")[0]), day);
            } else if (eventObject instanceof JSONArray) {
                JSONArray eventArray = events.getJSONArray(key);
                Day day = getDayFromJSONArray(eventArray, key);
                days.set(getDayIndex(key.split(",")[0]), day);
            }
        }

        return Schedule.builder()
                .allPairCount(allPairCount)
                .days(days)
                .build();
    }

    Day getDayFromJSONObject(JSONObject jsonObject, String key) {
        Day day = new Day();
        day.setFullDayName(key);
        List<List<Pair>> pairs = new ArrayList<>();

        for (String keyPair : jsonObject.keySet()) {
            JSONArray pairDataArr = jsonObject.getJSONArray(keyPair);
            getPairs(pairs, pairDataArr);
        }

        if (!pairs.isEmpty()) {
            Pair firstPair = pairs.get(0).get(0);
            LocalDate date = LocalDate.parse(firstPair.getDate(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            day.setDay(date.getDayOfMonth());
            day.setMonth(date.getMonthValue());
            day.setYear(date.getYear());
            day.setDayWeek(firstPair.getDayWeek());
        }
        day.setPairCount(pairs.size());
        day.setPairList(pairs);
        return day;
    }

    Day getDayFromJSONArray(JSONArray jsonArray, String key) {
        Day day = new Day();
        day.setFullDayName(key);
        List<List<Pair>> pairs = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray pairDataArr = jsonArray.getJSONArray(i);
            getPairs(pairs, pairDataArr);
        }

        if (!pairs.isEmpty()) {
            Pair firstPair = pairs.get(0).get(0);
            LocalDate date = LocalDate.parse(firstPair.getDate(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            day.setDay(date.getDayOfMonth());
            day.setMonth(date.getMonthValue());
            day.setYear(date.getYear());
            day.setDayWeek(firstPair.getDayWeek());
        }
        day.setPairCount(pairs.size());
        day.setPairList(pairs);
        return day;
    }

    private void getPairs(List<List<Pair>> pairs, JSONArray pairDataArr) {
        List<Pair> pairModule = new ArrayList<>();
        for (int j = 0; j < pairDataArr.length(); j++) {
            JSONObject pairData = pairDataArr.getJSONObject(j);

            JSONObject teacherJson = pairData.getJSONObject("teachers");
            List<Object> teachers = new ArrayList<>();
            for (String teacherKey : teacherJson.keySet()) {
                try {
                    Teacher teacher = gson.fromJson(teacherJson.getJSONObject(teacherKey).toString(), Teacher.class);
                    teachers.add(teacher);
                } catch (Exception ex) {
                    teachers.add(teacherKey);
                }
            }

            Pair pair = gson.fromJson(pairData.toString(), Pair.class);
            pair.setTeachers(teachers);
            pairModule.add(pair);
        }
        pairs.add(pairModule);
    }

    protected List<Pair> getPairNow(Schedule scheduleWeek) {
        LocalTime timeNow = LocalTime.now();
        LocalDate dateNow = LocalDate.now();

        for (Day day : scheduleWeek.getDays()) {
            if (day.getDay() != null && day.getDay() == dateNow.getDayOfMonth()) {
                log.info("day week - {}", day.getDay());
                LocalTime timeStartPair;
                LocalTime timeEndPair;

                for (List<Pair> pair : day.getPairList()) {
                    timeStartPair = LocalTime.parse(pair.get(0).getStartTime());
                    timeEndPair = LocalTime.parse(pair.get(0).getEndTime());

                    if (timeStartPair.isBefore(timeNow) && timeEndPair.isAfter(timeNow)) {
                        log.info("pair list now - {}", pair);
                        if (pair.isEmpty()) {
                            return new ArrayList<>();
                        } else {
                            return pair;
                        }
                    }
                }
            }
        }


        return new ArrayList<>();
    }


     int getDayIndex(String day) {
        return switch (day) {
            case "Понедельник" -> 0;
            case "Вторник" -> 1;
            case "Среда" -> 2;
            case "Четверг" -> 3;
            case "Пятница" -> 4;
            case "Суббота" -> 5;
            case "Воскресенье" -> 6;
            default -> throw new IllegalArgumentException("Некорректный день недели: " + day);
        };
    }

    String extractValueFromJson(String jsonString, String key) {
        String regex = key + "\":\"([^\"]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jsonString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    String extractValueFromHtml(String html, String key) {
        String regex = key + "&quot;:&quot;([^&quot;]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}