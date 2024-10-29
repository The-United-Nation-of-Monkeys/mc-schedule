package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.annotation.AnnotationExclusionStrategy;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.exception.ScheduleNotFoundException;
import com.project.ScheduleParsing.request.Fingerprint;
import com.project.ScheduleParsing.request.RequestGroup;
import com.project.ScheduleParsing.request.servermemo.*;
import com.project.ScheduleParsing.request.updates.Payload;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupScheduleService extends ScheduleService{

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private Integer numWeek;

    private final Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies(new AnnotationExclusionStrategy()).create();

    public Schedule getScheduleByGroup(String group, Integer week) {
        log.info("GroupScheduleService: start getScheduleByGroup(): group - {}, week - {}", group, week);
        try {
            RequestGroup requestGroup1 = firstConnectionToSchedule();
            RequestGroup requestGroup2 = secondConnectionToSchedule(requestGroup1);
            RequestGroup requestGroup3 = thirdConnectionToSchedule(requestGroup2, group, week);
            return lastConnectionToSchedule(requestGroup3);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new ScheduleNotFoundException(ex.getMessage());
        }
    }

    private RequestGroup firstConnectionToSchedule() throws IOException {
        log.info("GroupScheduleService: start firstConnectionToSchedule()");
        String scheduleUrl = "https://schedule.siriusuniversity.ru/";
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

        return buildFirstRequest();
    }

    private RequestGroup secondConnectionToSchedule(RequestGroup firstRequestGroup) throws IOException {
        log.info("GroupScheduleService: start secondConnectionToSchedule()");

        Connection.Response response = getConnection(firstRequestGroup);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");
        return buildSecondRequest();
    }

    private RequestGroup thirdConnectionToSchedule(RequestGroup secondRequestGroup, String group, Integer week) throws IOException {
        log.info("GroupScheduleService: start thirdConnectionToSchedule(): {}, {}", group, week);

        Connection.Response response = getConnection(secondRequestGroup);
        String responseBody = response.body();
        checkSum = extractValueFromJson(responseBody, "checksum");
        return buildThirdRequest(group, week);
    }

    private Schedule lastConnectionToSchedule(RequestGroup thirdRequestGroup) throws IOException {
        log.info("GroupScheduleService: start lastConnectionToSchedule()");

        Connection.Response lastResponse = getConnection(thirdRequestGroup);
        String responseBody = lastResponse.body();

        String startMarker = "\"<div wire:id=";
        String endMarker2 = "dirty";
        int startIndex = responseBody.indexOf(startMarker);
        int endIndex2 = responseBody.lastIndexOf(endMarker2);

        String r1 = responseBody.substring(0, startIndex+1);
        String r2 = responseBody.substring(endIndex2-3);
        String res = r1.concat(r2);

        return parseSchedule(res);
    }

    private Connection.Response getConnection(RequestGroup requestGroup) throws IOException {
        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/main-grid")
                .header("Content-Length", String.valueOf(1863))
                .header("Content-Type", "application/json")
                .header("Cookie", xsrfToken + "; " + siriusSession)
                .header("X-Csrf-Token", liveWireToken)
                .header("X-Livewire", "true")
                .requestBody(gson.toJson(requestGroup))
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .execute();
    }

    private RequestGroup buildFirstRequest() {
        Fingerprint fingerprint = new Fingerprint(wireId, "main-grid", "ru", "/", "GET", "acj");
        ServerMemo serverMemo = createServerMemo("", false, null, null);

        Payload payload = Payload.builder()
                .id("w887")
                .method("render")
                .params(new ArrayList<>())
                .build();
        Payload payload1 = Payload.builder()
                .id("rx9e")
                .method("$set")
                .params(List.of("width", 630))
                .build();
        Payload payload2 = Payload.builder()
                .id("ath1")
                .method("$set")
                .params(List.of("height", 705))
                .build();

        Update update = new Update("callMethod", payload);
        Update update1 = new Update("callMethod", payload1);
        Update update2 = new Update("callMethod", payload2);

        return new RequestGroup(fingerprint, serverMemo, List.of(update, update1, update2));
    }

    private RequestGroup buildSecondRequest() {
        Fingerprint fingerprint = new Fingerprint(wireId, "main-grid", "ru", "/", "GET", "acj");
        ServerMemo serverMemo = createServerMemo(null, true, 630, 705);

        Payload payload = Payload.builder()
                .id("w887")
                .method("render")
                .params(new ArrayList<>())
                .build();

        Update update = new Update("callMethod", payload);

        return new RequestGroup(fingerprint, serverMemo, List.of(update));
    }

    private RequestGroup buildThirdRequest(String group, Integer week) {
        Fingerprint fingerprint = new Fingerprint(wireId, "main-grid", "ru", "/", "GET", "acj");
        ServerMemo serverMemo = createServerMemo(null, true, 630, 705);

        Payload payload = Payload.builder()
                .id("w887")
                .method("set")
                .params(List.of(group))
                .build();

        List<Update> updates = new ArrayList<>();
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

        return new RequestGroup(fingerprint, serverMemo, updates);
    }

    private ServerMemo createServerMemo(String search, boolean statusInit, Integer width, Integer height) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Month month = Month.builder()
                .number(String.valueOf(now.getMonthValue()))
                .full(Months.values()[now.getMonthValue()-1].toString())
                .fullForDisplay(MonthForFront.values()[now.getMonthValue()-1].toString())
                .build();

        DataForGroup dataForGroup = DataForGroup.builder()
                .year(String.valueOf(now.getYear()))
                .date(now.format(formatter))
                .month(month)
                .numWeek(numWeek)
                .addNumWeek(0)
                .minusNumWeek(0)
                .count(0)
                .type("grid")
                .search(search)
                .group("")
                .groupList(new ArrayList<>())
                .events(new ArrayList<>())
                .eventElement(new ArrayList<>())
                .watching(false)
                .currentRouteName("grid")
                .statusInit(statusInit)
                .lectures(true)
                .seminars(true)
                .practices(true)
                .laboratories(true)
                .exams(true)
                .other(true)
                .width(width)
                .height(height)
                .build();

        List<Object> ids = new ArrayList<>();
        for (int i = 0; i < 20; i++){
            ids.add(null);
        }

        GroupList groupList = GroupList.builder()
                .className("App\\Models\\Group")
                .id(ids)
                .relations(new ArrayList<>())
                .connection("mysql")
                .build();

        ModelCollections modelCollections = new ModelCollections(groupList);

        DataMetaForGroup dataMetaForGroup = new DataMetaForGroup(modelCollections);

        return ServerMemo.builder()
                .children(new ArrayList<>())
                .errors(new ArrayList<>())
                .htmlHash(htmlHash)
                .data(dataForGroup)
                .dataMeta(dataMetaForGroup)
                .checksum(checkSum)
                .build();
    }
}