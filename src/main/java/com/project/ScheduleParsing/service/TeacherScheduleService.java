package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.annotation.AnnotationExclusionStrategy;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.exception.ScheduleNotFoundException;
import com.project.ScheduleParsing.repository.TeacherRepository;
import com.project.ScheduleParsing.request.*;
import com.project.ScheduleParsing.request.servermemo.*;
import com.project.ScheduleParsing.request.updates.Payload;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherScheduleService {

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private Integer numWeek;

    private final Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

    private final TeacherRepository teacherRepository;

    public Schedule getScheduleByTeacher(String teacher, int week) {
        log.info("TeacherScheduleService: start getScheduleByTeacher(): {}, {}", teacher, week);

        try {
            String request1 = firstConnectionToSchedule();
            RequestTeacherAndAuditory requestTeacherAndAuditory2 = secondConnectionToSchedule(teacher, week, request1);
            return finalConnectionToSchedule(requestTeacherAndAuditory2);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new ScheduleNotFoundException("Во время поиска расписания произошла ошибка, повторите попытку позже.");
        }
    }

    public String firstConnectionToSchedule() throws IOException {
        log.info("TeacherScheduleService: start firstConnectionToSchedule()");

        String scheduleUrl = "https://schedule.siriusuniversity.ru/teacher";
        Connection.Response response1 = Jsoup.connect(scheduleUrl)
                .method(Connection.Method.GET)
                .execute();

        Map<String, String> headers = response1.headers();
        Document doc = response1.parse();
        Element scriptElement = doc.selectFirst("script:containsData(window.livewire_token)"); // Находим скрипт с токеном
        if (scriptElement != null) {
            String scriptContent = scriptElement.html();
            liveWireToken = scriptContent.replaceAll(".*window\\.livewire_token = '(.*?)';.*", "$1");
        }

        String jsonData = doc.html().split("wire:initial-data=\"")[1].split("\"")[0]
                .replace("&quot;", "\"");

        JSONObject jsonObject = new JSONObject(jsonData);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
        numWeek = data.getInt("numWeek");

        xsrfToken = headers.get("Set-Cookie").trim().split(";")[0];
        siriusSession = "raspisanie_universitet_sirius_session="+response1.cookie("raspisanie_universitet_sirius_session");

        Element element1 = doc.selectFirst("div[wire:id]");
        htmlHash = extractValueFromHtml(doc.html(), "htmlHash");
        checkSum = extractValueFromHtml(doc.html(), "checksum");
        if (element1 != null) {
            wireId = element1.attr("wire:id");
        }

        return jsonData.substring(0, jsonData.length()-1).concat("""
                ,
                  "updates": [
                    {
                      "type": "callMethod",
                      "payload": {
                        "id": "kduu",
                        "method": "render",
                        "params": []
                      }
                    },
                    {
                      "type": "callMethod",
                      "payload": {
                        "id": "wa2s",
                        "method": "$set",
                        "params": [
                          "width",
                          630
                        ]
                      }
                    },
                    {
                      "type": "callMethod",
                      "payload": {
                        "id": "ocot",
                        "method": "$set",
                        "params": [
                          "height",
                          705
                        ]
                      }
                    }
                  ]
                }""");
    }

    public RequestTeacherAndAuditory secondConnectionToSchedule(String teacherName, int week, String firstRequest) throws IOException {
        log.info("TeacherScheduleService: start secondConnectionToSchedule()");

        Connection.Response response = getConnection(firstRequest);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");

        int index1 = response.body().indexOf("\"teachersList\"");
        String str1 = response.body().substring(index1);
        int index2 = str1.indexOf("\"statusInit\"");
        String str2 = "{"+ str1.substring(0, index2-1) + "}";

        DataTeacher response1 = gson.fromJson(str2, DataTeacher.class);

        Map<String, Object> teachersList = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : response1.getTeachersList().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Teacher teacher = gson.fromJson(gson.toJson(value), Teacher.class);
                teachersList.put(teacher.getId(), teacher);
            } else if (value instanceof String) {
                teachersList.put(key, value);
            }
        }

        return buildSecondRequest(teacherName, week, teachersList);
    }

    public Schedule finalConnectionToSchedule(RequestTeacherAndAuditory firstRequest) throws IOException {
        log.info("TeacherScheduleService: start finalConnectionToSchedule()");

        Connection.Response response = getConnection(gson.toJson(firstRequest));
        String responseBody = response.body();
        String htmlSchedule = StringEscapeUtils.unescapeJava(responseBody);

        String startMarker = "\"<div wire:id=\"";
        String endMarker2 = "</div>";
        int startIndex = htmlSchedule.indexOf(startMarker);
        int endIndex2 = htmlSchedule.lastIndexOf(endMarker2);

        String r1 = htmlSchedule.substring(0, startIndex+1);
        String r2 = htmlSchedule.substring(endIndex2+7);
        String res = r1.concat(r2);

        return parseSchedule(res);
    }

    public Schedule parseSchedule(String json) {
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

    private Day getDayFromJSONObject(JSONObject jsonObject, String key) {
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
            List<Teacher> teachers = new ArrayList<>();
            for (String teacherKey : teacherJson.keySet()) {
                Teacher teacher = gson.fromJson(teacherJson.getJSONObject(teacherKey).toString(), Teacher.class);
                teachers.add(teacher);
            }

            Pair pair = gson.fromJson(pairData.toString(), Pair.class);
            pair.setTeachers(teachers);
            pairs.add(List.of(pair));
        }

        day.setPairCount(pairs.size());
        day.setPairList(pairs);
        return day;
    }

    private Day getDayFromJSONArray(JSONArray jsonArray, String key) {
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
                List<Teacher> teachers = new ArrayList<>();
                for (String teacherKey : teacherJson.keySet()) {
                    Teacher teacher = gson.fromJson(teacherJson.getJSONObject(teacherKey).toString(), Teacher.class);
                    teachers.add(teacher);
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

    private int getDayIndex(String day) {
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

    private Connection.Response getConnection(String request) throws IOException {
        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/teachers.teacher-main-grid")
                .header("Content-Length", String.valueOf(10786))
                .header("Content-Type", "application/json")
                .header("Cookie", xsrfToken + "; " + siriusSession)
                .header("X-Csrf-Token", liveWireToken)
                .header("X-Livewire", "true")
                .requestBody(request)
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .execute();
    }

    private RequestTeacherAndAuditory buildSecondRequest(String teacherName, int week, Map<String, Object> teachersList) {
        Fingerprint fingerprint = new Fingerprint(wireId, "teachers.teacher-main-grid", "ru", "teacher", "GET", "acj");
        Effects effects = new Effects();
        ServerMemo serverMemo = createServerMemo(teachersList);

        List<Update> updates = new ArrayList<>();
        Payload payload = Payload.builder()
                .id("s7q8i")
                .method("set")
                .params(List.of(getTeacherId(teacherName)))
                .build();
        updates.add(new Update("callMethod", payload));

        if (week > 0 ) {
            for (int i = 0; i < week; i++){
                Payload newPayload = Payload.builder()
                        .id("w887")
                        .method("addWeek")
                        .params(new ArrayList<>())
                        .build();

                updates.add(new Update("callMethod", newPayload));
            }
        } else if (week < 0) {
            for (int i = week; i < 0; i++){
                Payload newPayload = Payload.builder()
                        .id("w887")
                        .method("minusWeek")
                        .params(new ArrayList<>())
                        .build();

                updates.add(new Update("callMethod", newPayload));
            }
        }

        return new RequestTeacherAndAuditory(fingerprint, effects, serverMemo, updates);
    }

    private ServerMemo createServerMemo(Map<String, Object> teachersList) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Month month = Month.builder()
                .number(String.valueOf(now.getMonthValue()))
                .full(Months.values()[now.getMonthValue()-1].toString())
                .fullForDisplay(MonthForFront.values()[now.getMonthValue()-1].toString())
                .build();

        DataForTeacher dataForGroup = DataForTeacher.builder()
                .year(String.valueOf(now.getYear()))
                .date(now.format(formatter))
                .month(month)
                .numWeek(numWeek)
                .addNumWeek(0)
                .minusNumWeek(0)
                .count(0)
                .type("grid")
                .gridRoute("schedule.teachers.grid")
                .listRoute("schedule.teachers.main")
                .search(null)
                .teacherName("")
                .teacher("")
                .teacherShow(true)
                .teachersList(teachersList)
                .events(new ArrayList<>())
                .eventElement(new ArrayList<>())
                .watching(false)
                .currentRouteName("teacher.grid")
                .statusInit(true)
                .lectures(true)
                .seminars(true)
                .practices(true)
                .laboratories(true)
                .exams(true)
                .other(true)
                .width(630)
                .height(705)
                .build();

        DataMetaForTeacherAndAuditory dataMetaForGroup = DataMetaForTeacherAndAuditory.builder()
                .collections(List.of("teachersList"))
                .build();

        return ServerMemo.builder()
                .children(new ArrayList<>())
                .errors(new ArrayList<>())
                .htmlHash(htmlHash)
                .data(dataForGroup)
                .dataMeta(dataMetaForGroup)
                .checksum(checkSum)
                .build();
    }

    private String extractValueFromHtml(String html, String key) {
        String regex = key + "&quot;:&quot;([^&quot;]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractValueFromJson(String jsonString, String key) {
        String regex = key + "\":\"([^\"]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jsonString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getTeacherId(String fio) {
        com.project.ScheduleParsing.model.Teacher teacher = teacherRepository.findTeacherByFio(fio);
        return String.valueOf(teacher.getId());
    }
}