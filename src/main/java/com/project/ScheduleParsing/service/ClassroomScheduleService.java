package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.annotation.AnnotationExclusionStrategy;
import com.project.ScheduleParsing.dto.ClassroomListResponse;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.exception.ScheduleNotFoundException;
import com.project.ScheduleParsing.request.Effects;
import com.project.ScheduleParsing.request.Fingerprint;
import com.project.ScheduleParsing.request.RequestTeacherAndAuditory;
import com.project.ScheduleParsing.request.servermemo.*;
import com.project.ScheduleParsing.request.updates.Payload;
import com.project.ScheduleParsing.request.updates.PayloadForTeacher;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomScheduleService extends ScheduleService{

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private Integer numWeek;

    private final Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

    public ClassroomListResponse getClassrooms(String search) {
        log.info("ClassroomScheduleService: start getClassrooms(): {}", search);

        try {
            String firstRequest = firstConnectionToSchedule();
            RequestTeacherAndAuditory secondRequest = secondConnectionToSchedule("auditory", search, null, 0, firstRequest);
            return lastConnectionToClassrooms(secondRequest);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new ScheduleNotFoundException(ex.getMessage());
        }
    }

    public Schedule getScheduleByClassroom(String auditory, int week) {
        log.info("ClassroomScheduleService: start getScheduleByAuditory(): {}, {}", auditory, week);

        try {
            String firstRequest = firstConnectionToSchedule();
            RequestTeacherAndAuditory secondRequest = secondConnectionToSchedule("schedule", null, auditory, week, firstRequest);
            return lastConnectionToSchedule(secondRequest);
        } catch (IOException e) {
            throw new ScheduleNotFoundException(e.getMessage());
        }
    }

    public List<Pair> getScheduleByClassroomNow(String auditory) {
        log.info("ClassroomScheduleService: start getScheduleByAuditoryNow(): {}", auditory);

        Schedule scheduleWeek = getScheduleByClassroom(auditory, 0);
        return getPairNow(scheduleWeek);
    }

    private String firstConnectionToSchedule() throws IOException {
        log.info("ClassroomScheduleService: start firstConnectionToSchedule()");

        String scheduleUrl = "https://schedule.siriusuniversity.ru/classroom";
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

    private RequestTeacherAndAuditory secondConnectionToSchedule(String typeRequest, String search, String auditory, int week, String firstRequestGroup) throws IOException {
        log.info("ClassroomScheduleService: start secondConnectionToSchedule()");

        Connection.Response response = getConnection(firstRequestGroup);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");

        JSONObject jsonObject = new JSONObject(firstRequestGroup);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
        JSONArray classroomArray = data.getJSONArray("classroomsList");
        List<String> classroomList = classroomArray.toList().stream().map(String::valueOf).toList();

        return buildSecondRequest(typeRequest, search, auditory, week, classroomList);
    }

    private Schedule lastConnectionToSchedule(RequestTeacherAndAuditory request) throws IOException {
        log.info("ClassroomScheduleService: start lastConnectionToSchedule()");

        Connection.Response response = getConnection(gson.toJson(request));
        String responseBody = response.body();

        String startMarker = "\"<div wire:id=";
        String endMarker2 = "dirty";
        int startIndex = responseBody.indexOf(startMarker);
        int endIndex2 = responseBody.lastIndexOf(endMarker2);

        String r1 = responseBody.substring(0, startIndex+1);
        String r2 = responseBody.substring(endIndex2-3);
        String res = r1.concat(r2);

        return parseSchedule(res);
    }

    private ClassroomListResponse lastConnectionToClassrooms(RequestTeacherAndAuditory request) throws IOException {
        log.info("ClassroomScheduleService: start lastConnectionToClassrooms()");

        Connection.Response response = getConnection(gson.toJson(request));
        String responseBody = response.body();

        String startMarker = "\"<div wire:id=";
        String endMarker2 = "dirty";
        int startIndex = responseBody.indexOf(startMarker);
        int endIndex2 = responseBody.lastIndexOf(endMarker2);

        String r1 = responseBody.substring(0, startIndex+1);
        String r2 = responseBody.substring(endIndex2-3);
        String res = r1.concat(r2);

        JSONObject jsonObject = new JSONObject(res);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
        JSONObject classroomArray = data.getJSONObject("classroomsList");
        List<String> classroomList = classroomArray.keySet().stream().map(k -> classroomArray.get(k).toString()).toList();

        return new ClassroomListResponse(classroomList.size(), classroomList);
    }

    private Connection.Response getConnection(String request) throws IOException {
        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/classroom.classroom-main-grid")
                .header("Content-Length", String.valueOf(1184))
                .header("Content-Type", "application/json")
                .header("Cookie", xsrfToken + "; " + siriusSession)
                .header("X-Csrf-Token", liveWireToken)
                .header("X-Livewire", "true")
                .requestBody(request)
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .execute();
    }

    private RequestTeacherAndAuditory buildSecondRequest(String typeRequest, String search, String auditoryName, int week, List<String> classroomsList) {
        Fingerprint fingerprint = new Fingerprint(wireId, "classroom.classroom-main-grid", "ru", "classroom", "GET", "acj");
        Effects effects = new Effects();
        ServerMemo serverMemo = createServerMemo(classroomsList);

        List<Update> updates;
        if (typeRequest.equals("schedule")) {
            updates = createUpdates(auditoryName, week);
        } else {
            updates = createUpdates(search);
        }

        return new RequestTeacherAndAuditory(fingerprint, effects, serverMemo, updates);
    }

    private ServerMemo createServerMemo(List<String> classroomsList) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Month month = Month.builder()
                .number(String.valueOf(now.getMonthValue()).length() == 1 ?
                        "0" + now.getMonthValue() :
                        String.valueOf(now.getMonthValue()))
                .full(Months.values()[now.getMonthValue()-1].toString())
                .fullForDisplay(MonthForFront.values()[now.getMonthValue()-1].toString())
                .build();

        DataForAuditory dataForGroup = DataForAuditory.builder()
                .year(String.valueOf(now.getYear()))
                .date(now.format(formatter))
                .month(month)
                .numWeek(numWeek)
                .addNumWeek(0)
                .minusNumWeek(0)
                .count(0)
                .type("grid")
                .gridRoute("schedule.classroom.grid")
                .listRoute("schedule.classroom.main")
                .search(null)
                .classroom("")
                .classroomsList(classroomsList)
                .currentRouteName("classroom.grid")
                .watching(false)
                .events(new ArrayList<>())
                .eventElement(new ArrayList<>())
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
                .collections(List.of("classroomsList"))
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

    private List<Update> createUpdates(String auditoryName, int week) {
        List<Update> updates = new ArrayList<>();
        Payload payload = Payload.builder()
                .id("s7q8i")
                .method("set")
                .params(List.of(auditoryName))
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
        return updates;
    }

    private List<Update> createUpdates(String search) {
        List<Update> updates = new ArrayList<>();
        PayloadForTeacher payload = PayloadForTeacher.builder()
                .id("gqky")
                .name("search")
                .value(search)
                .build();
        updates.add(new Update("syncInput", payload));
        return updates;
    }
}
