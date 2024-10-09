package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.exception.ScheduleNotFoundException;
import com.project.ScheduleParsing.request.Fingerprint;
import com.project.ScheduleParsing.request.Request;
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
import org.jsoup.select.Elements;
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
public class GroupScheduleService {

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private Integer numWeek;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public Schedule getScheduleByGroup(String group, Integer week) {
        log.info("GroupScheduleService: getScheduleByGroup(): group - {}, week - {}", group, week);
        try {
            Request request1 = firstConnectionToSchedule();
            Request request2 = secondConnectionToSchedule(request1);
            Request request3 = thirdConnectionToSchedule(request2, group, week);
            return lastConnectionToSchedule(request3);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new ScheduleNotFoundException(ex.getMessage());
        }
    }

    public Request firstConnectionToSchedule() throws IOException {
        log.info("GroupScheduleService: firstConnectionToSchedule()");
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
        log.info(extractValueFromHtml(doc.html(), "numWeek"));
        if (element1 != null) {
            wireId = element1.attr("wire:id");
        }

        return buildFirstRequest();
    }

    public Request secondConnectionToSchedule(Request firstRequest) throws IOException {
        log.info("GroupScheduleService: secondConnectionToSchedule()");

        Connection.Response response = getConnection(firstRequest);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");
        return buildSecondRequest();
    }

    public Request thirdConnectionToSchedule(Request secondRequest, String group, Integer week) throws IOException {
        log.info("GroupScheduleService: thirdConnectionToSchedule()");

        Connection.Response response = getConnection(secondRequest);
        String responseBody = response.body();
        checkSum = extractValueFromJson(responseBody, "checksum");
        return buildThirdRequest(group, week);
    }

    public Schedule lastConnectionToSchedule(Request thirdRequest) throws IOException {
        log.info("GroupScheduleService: lastConnectionToSchedule()");

        Connection.Response lastResponse = getConnection(thirdRequest);

        String responseBody = lastResponse.body();
        String htmlSchedule = StringEscapeUtils.unescapeJava(responseBody);

        String startMarker = "{\"effects\":{\"html\":\"<div wire:id=\"";
        String endMarker = "<div wire:id";
        String endMarker2 = "</div>";

        int startIndex = htmlSchedule.indexOf(startMarker);
        int endIndex = htmlSchedule.indexOf(endMarker, startIndex);
        int endIndex2 = htmlSchedule.lastIndexOf(endMarker2);

        htmlSchedule = htmlSchedule.substring(0, startIndex) + htmlSchedule.substring(endIndex);
        htmlSchedule = htmlSchedule.substring(0, endIndex2-14);

        return parseSchedule(htmlSchedule);
    }

    private Connection.Response getConnection(Request request) throws IOException {
        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/main-grid")
                .header("Content-Length", String.valueOf(1184))
                .header("Content-Type", "application/json")
                .header("Cookie", xsrfToken + "; " + siriusSession)
                .header("X-Csrf-Token", liveWireToken)
                .header("X-Livewire", "true")
                .requestBody(gson.toJson(request))
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .execute();
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

    private String extractValueFromHtml(String html, String key) {
        String regex = key + "&quot;:&quot;([^&quot;]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Request buildFirstRequest() {
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

        return new Request(fingerprint, serverMemo, List.of(update, update1, update2));
    }

    private Request buildSecondRequest() {
        Fingerprint fingerprint = new Fingerprint(wireId, "main-grid", "ru", "/", "GET", "acj");
        ServerMemo serverMemo = createServerMemo(null, true, 630, 705);

        Payload payload = Payload.builder()
                .id("w887")
                .method("render")
                .params(new ArrayList<>())
                .build();

        Update update = new Update("callMethod", payload);

        return new Request(fingerprint, serverMemo, List.of(update));
    }

    private Request buildThirdRequest(String group, Integer week) {
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
            Payload newPayload = Payload.builder()
                    .id("w887")
                    .method("minusWeek")
                    .params(new ArrayList<>())
                    .build();

            updates.add(new Update("callMethod", newPayload));
        }

        return new Request(fingerprint, serverMemo, updates);
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

    private Schedule parseSchedule(String htmlSchedule) {
        List<Day> days = new ArrayList<>();
        Document document = Jsoup.parse(htmlSchedule);
        Elements date = document.select("div.text-sm.font-bold.text-gray-500.pb-2");
        Elements headers = document.select("div.text-sm.font-bold.text-gray-500.pb-2");
        Element lastPair = document.select("div.display-contents").first();

        for (int j = 1; j < headers.size() + 1; j++) {
            Element firstHeader = headers.get(j-1);
            Element secondHeader;

            if (j == headers.size()) {
                secondHeader = lastPair;
            } else {
                secondHeader = headers.get(j);
            }

            Element currentElement = firstHeader.nextElementSibling();
            StringBuilder result = new StringBuilder();
            while (currentElement != null && !currentElement.equals(secondHeader)) {
                result.append(currentElement.outerHtml());
                currentElement = currentElement.nextElementSibling();
            }

            Document docPair = Jsoup.parse(result.toString());
            int size = docPair.select(".text-gray-400.text-sm.pl-1").size();
            List<Pair> pairs = new ArrayList<>();

            for(int i = 0; i < size; i++) {
                String pairNumber = docPair.select(".text-gray-500.font-bold.pr-2.text-sm").get(i).text();
                String pairType = docPair.select(".text-gray-400.text-sm.pl-1").get(i).text();
                String pairName = docPair.select(".text-gray-500.font-bold.m-3.mt-0.relative.text-sm").get(i).text();
                String pairTime = docPair.select(".ml-auto.text-sm").get(i).text();
                Elements pairTeachers = docPair.select(".text-gray-400.p-3.pt-0.pl-8.text-sm span.line-clamp-2");
                Elements pairLocation = docPair.select(".relative.text-gray-500.p-3.pt-0.flex.text-sm .font-bold");

                Optional<String> optTeacher = (i < pairTeachers.size()) ? Optional.of(pairTeachers.get(i).text()) : Optional.empty();
                Optional<String> optLocation = (i < pairLocation.size()) ? Optional.of(pairLocation.get(i).text()) : Optional.empty();

                pairs.add(Pair.builder()
                        .pairNumber(Integer.valueOf(pairNumber))
                        .pairType(pairType)
                        .pairName(pairName)
                        .pairTeacher(optTeacher.orElse(""))
                        .pairLocation(optLocation.orElse(""))
                        .pairTime(pairTime)
                        .build());
            }

            String[] fullDate = date.get(j - 1).text().split(", ")[1].split(" ");
            LocalDate localDate = LocalDate.of(2024, MonthForFront.valueOf(fullDate[1]).ordinal() + 1, Integer.parseInt(fullDate[0]));

            days.add(Day.builder()
                    .day(localDate.getDayOfMonth())
                    .month(localDate.getMonthValue())
                    .pairCount(pairs.size())
                    .pairs(pairs)
                    .build());
        }

        return Schedule.builder()
                .days(days)
                .build();
    }
}
