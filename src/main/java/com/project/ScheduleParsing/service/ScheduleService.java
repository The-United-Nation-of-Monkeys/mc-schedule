package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.annotation.AnnotationExclusionStrategy;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.dto.Teacher;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ScheduleService {

    private final Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

    Schedule parseSchedule(String json) {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
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
            } else {
                days.set(getDayIndex(key.split(",")[0]), null);
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

        boolean first = true;
        for (String pairKey : jsonObject.keySet()) {
            JSONObject pairData = (JSONObject) jsonObject.getJSONArray(pairKey).get(0);

            LocalDate date = LocalDate.parse(pairData.getString("date"), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (first) {
                day.setDay(date.getDayOfMonth());
                day.setMonth(date.getMonthValue());
                day.setYear(date.getYear());
                day.setDayWeek(pairData.getString("dayWeek"));
                first = false;
            }

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
            pairs.add(List.of(pair));
        }

        day.setPairCount(pairs.size());
        day.setPairList(pairs);
        return day;
    }

    Day getDayFromJSONArray(JSONArray jsonArray, String key) {
        Day day = new Day();
        day.setFullDayName(key);
        List<List<Pair>> pairs = new ArrayList<>();

        boolean first = true;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray pairDataArr = jsonArray.getJSONArray(i);
            List<Pair> pairModule = new ArrayList<>();
            for (int j = 0; j < pairDataArr.length(); j++) {
                JSONObject pairData = pairDataArr.getJSONObject(j);

                LocalDate date = LocalDate.parse(pairData.getString("date"), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                if (first) {
                    day.setDay(date.getDayOfMonth());
                    day.setMonth(date.getMonthValue());
                    day.setYear(date.getYear());
                    day.setDayWeek(pairData.getString("dayWeek"));
                    first = false;
                }

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

        day.setPairCount(pairs.size());
        day.setPairList(pairs);
        return day;
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