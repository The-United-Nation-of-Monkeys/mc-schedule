package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.request.Fingerprint;
import com.project.ScheduleParsing.request.Request;
import com.project.ScheduleParsing.request.servermemo.*;
import com.project.ScheduleParsing.request.updates.Payload;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScheduleParsingService {

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public void getSchedule(String group, Integer week) throws IOException {
        Request request1 = firstConnectionToSchedule();
        Request request2 = secondConnectionToSchedule(request1);
        Request request3 = thirdConnectionToSchedule(request2, group, week);
        lastConnectionToSchedule(request3);
    }

    public Request firstConnectionToSchedule() throws IOException {
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

    public Request secondConnectionToSchedule(Request firstRequest) throws IOException {
        Connection.Response response2 = getConnection(firstRequest);
        String decodedString = StringEscapeUtils.unescapeJava(response2.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");
        return buildSecondRequest();
    }

    public Request thirdConnectionToSchedule(Request secondRequest, String group, Integer week) throws IOException {
        Connection.Response response3 = getConnection(secondRequest);
        String responseBody3 = response3.body();
        checkSum = extractValueFromJson(responseBody3, "checksum");
        return buildThirdRequest(group, week);
    }

    public void lastConnectionToSchedule(Request thirdRequest) throws IOException {
        Connection.Response response4 = getConnection(thirdRequest);

        String responseBody4 = response4.body();
        String decodedString4 = StringEscapeUtils.unescapeJava(responseBody4);

        String startMarker = "{\"effects\":{\"html\":\"<div wire:id=\"";
        String endMarker = "<div wire:id";
        String endMarker2 = "</div>";

        int startIndex = decodedString4.indexOf(startMarker);
        int endIndex = decodedString4.indexOf(endMarker, startIndex);
        int endIndex2 = decodedString4.lastIndexOf(endMarker2);

        decodedString4 = decodedString4.substring(0, startIndex) + decodedString4.substring(endIndex);
        decodedString4 = decodedString4.substring(0, endIndex2-14);
        System.out.println(decodedString4);

        Document document = Jsoup.parse(decodedString4);

        Elements date = document.select("div.text-sm.font-bold.text-gray-500.pb-2");
        Elements headers = document.select("div.text-sm.font-bold.text-gray-500.pb-2");

        for (int j = 1; j < headers.size(); j++) {
            //day of week
            System.out.println(date.get(j-1).text());

            Element firstHeader = headers.get(j-1);
            Element secondHeader = headers.get(j);

            Element currentElement = firstHeader.nextElementSibling();

            StringBuilder result = new StringBuilder();

            while (currentElement != null && !currentElement.equals(secondHeader)) {
                result.append(currentElement.outerHtml());
                currentElement = currentElement.nextElementSibling();
            }

            Document docPair = Jsoup.parse(result.toString());
            int size = docPair.select(".text-gray-400.text-sm.pl-1").size();

            for(int i = 0; i < size; i++) {
                String pairNumber = docPair.select(".text-gray-500.font-bold.pr-2.text-sm").get(i).text();
                String pairType = docPair.select(".text-gray-400.text-sm.pl-1").get(i).text();
                String pairName = docPair.select(".text-gray-500.font-bold.m-3.mt-0.relative.text-sm").get(i).text();
                String pairTeachers = docPair.select(".text-gray-400.p-3.pt-0.pl-8.text-sm span").get(i).text();
                String pairLocation = docPair.select(".relative.text-gray-500.p-3.pt-0.flex.text-sm .font-bold").get(i).text();
                String pairTime = docPair.select(".ml-auto.text-sm").get(i).text();

                System.out.println(pairNumber + " | " + pairType + " | " + pairName + " | " + pairTeachers + " | " + pairLocation + " | " + pairTime);

            }
        }

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

        Data data = Data.builder()
                .year(String.valueOf(now.getYear()))
                .date(now.format(formatter))
                .month(month)
                .numWeek(6)
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

        DataMeta dataMeta = new DataMeta(modelCollections);

        return ServerMemo.builder()
                .children(new ArrayList<>())
                .errors(new ArrayList<>())
                .htmlHash(htmlHash)
                .data(data)
                .dataMeta(dataMeta)
                .checksum(checkSum)
                .build();
    }

    private Schedule parseSchedule(String schedule) {


        return null;
    }
}
