package com.project.ScheduleParsing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.dto.Schedule;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public Schedule getScheduleByTeacher(String teacher, int week) {
        try {
            Request request1 = firstConnectionToSchedule();
            Request request2 = secondConnectionToSchedule(request1);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

//        Request request3 = thirdConnectionToSchedule(request2, group, week);
//        lastConnectionToSchedule(request3);



        return null;
    }

    public Request firstConnectionToSchedule() throws IOException {
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

        log.info(jsonData);

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
        log.info("{}, {}, {}", wireId, htmlHash, checkSum);

        return buildFirstRequest();
    }

    public Request secondConnectionToSchedule(Request firstRequest) throws IOException {
        log.info("GroupScheduleService: secondConnectionToSchedule()");

        log.info(gson.toJson(firstRequest));
        log.info("{\"fingerprint\":{\"id\":\"LimdZVgR3lHDzKqr0Wbw\",\"name\":\"teachers.teacher-main-grid\",\"locale\":\"ru\",\"path\":\"teacher\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"ced96434\",\"data\":{\"year\":\"2024\",\"date\":\"08.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":6,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"gridRoute\":\"schedule.teachers.grid\",\"listRoute\":\"schedule.teachers.main\",\"search\":null,\"teacherName\":\"\",\"teacher\":\"\",\"teacherShow\":true,\"teachersList\":{\"Бородин П.М.\":\"Бородин П.М.\",\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\":{\"id\":\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\",\"last_name\":\"Карабельский\",\"first_name_one\":\"А\",\"first_name\":\"Александр\",\"middle_name\":\"Владимирович\",\"middle_name_one\":\"В\",\"fio\":\"Карабельский Александр Владимирович\",\"department_fio\":\"Карабельский Александр Владимирович (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"cefddf57-4108-4bfa-8f78-e43dd8639b1c\":{\"id\":\"cefddf57-4108-4bfa-8f78-e43dd8639b1c\",\"last_name\":\"Чечушков\",\"first_name_one\":\"А\",\"first_name\":\"Антон\",\"middle_name\":\"Владимирович\",\"middle_name_one\":\"В\",\"fio\":\"Чечушков Антон Владимирович\",\"department_fio\":\"Чечушков Антон Владимирович (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"9543d3e9-6e54-42b8-879c-98d73a6ba63e\":{\"id\":\"9543d3e9-6e54-42b8-879c-98d73a6ba63e\",\"last_name\":\"Минская\",\"first_name_one\":\"Е\",\"first_name\":\"Екатерина\",\"middle_name\":\"Сергеевна\",\"middle_name_one\":\"С\",\"fio\":\"Минская Екатерина Сергеевна\",\"department_fio\":\"Минская Екатерина Сергеевна (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"173da7a5-635b-42d0-807f-58292941979f\":{\"id\":\"173da7a5-635b-42d0-807f-58292941979f\",\"last_name\":\"Манахов\",\"first_name_one\":\"А\",\"first_name\":\"Андрей\",\"middle_name\":\"Дмитриевич\",\"middle_name_one\":\"Д\",\"fio\":\"Манахов Андрей Дмитриевич\",\"department_fio\":\"Манахов Андрей Дмитриевич (Направление \\\"Генетика\\\")\",\"department\":\"Направление \\\"Генетика\\\"\"},\"9a75161c-68b2-4431-826e-f1b7d92b631f\":{\"id\":\"9a75161c-68b2-4431-826e-f1b7d92b631f\",\"last_name\":\"Месонжник\",\"first_name_one\":\"Н\",\"first_name\":\"Наталья\",\"middle_name\":\"Владимировна\",\"middle_name_one\":\"В\",\"fio\":\"Месонжник Наталья Владимировна\",\"department_fio\":\"Месонжник Наталья Владимировна (Ресурсный центр аналитических методов)\",\"department\":\"Ресурсный центр аналитических методов\"},\"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\":{\"id\":\"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\",\"last_name\":\"Якшин\",\"first_name_one\":\"Д\",\"first_name\":\"Дмитрий\",\"middle_name\":\"Михайлович\",\"middle_name_one\":\"М\",\"fio\":\"Якшин Дмитрий Михайлович\",\"department_fio\":\"Якшин Дмитрий Михайлович (Ресурсный центр генетической инженерии)\",\"department\":\"Ресурсный центр генетической инженерии\"},\"6a6578d5-b080-4bce-8366-b0d01e893a61\":{\"id\":\"6a6578d5-b080-4bce-8366-b0d01e893a61\",\"last_name\":\"Колесова\",\"first_name_one\":\"Е\",\"first_name\":\"Екатерина\",\"middle_name\":\"Петровна\",\"middle_name_one\":\"П\",\"fioShow more");

        Connection.Response response = getConnection(firstRequest);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");
        return buildSecondRequest();
    }

    private Connection.Response getConnection(Request request) throws IOException {
        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/teachers.teacher-main-grid")
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
        Fingerprint fingerprint = new Fingerprint(wireId, "teachers.teacher-main-grid", "ru", "teacher", "GET", "acj");
        ServerMemo serverMemo = createServerMemo(null, false, null, null);

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
        Fingerprint fingerprint = new Fingerprint(wireId, "teachers.teacher-main-grid", "ru", "teacher", "GET", "acj");
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

        String teachers = "{\"teachersList\": {\n" +
                "        \"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\": {\n" +
                "          \"id\": \"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\",\n" +
                "          \"last_name\": \"Карабельский\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Александр\",\n" +
                "          \"middle_name\": \"Владимирович\",\n" +
                "          \"middle_name_one\": \"В\",\n" +
                "          \"fio\": \"Карабельский Александр Владимирович\",\n" +
                "          \"department_fio\": \"Карабельский Александр Владимирович (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"cefddf57-4108-4bfa-8f78-e43dd8639b1c\": {\n" +
                "          \"id\": \"cefddf57-4108-4bfa-8f78-e43dd8639b1c\",\n" +
                "          \"last_name\": \"Чечушков\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Антон\",\n" +
                "          \"middle_name\": \"Владимирович\",\n" +
                "          \"middle_name_one\": \"В\",\n" +
                "          \"fio\": \"Чечушков Антон Владимирович\",\n" +
                "          \"department_fio\": \"Чечушков Антон Владимирович (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"9543d3e9-6e54-42b8-879c-98d73a6ba63e\": {\n" +
                "          \"id\": \"9543d3e9-6e54-42b8-879c-98d73a6ba63e\",\n" +
                "          \"last_name\": \"Минская\",\n" +
                "          \"first_name_one\": \"Е\",\n" +
                "          \"first_name\": \"Екатерина\",\n" +
                "          \"middle_name\": \"Сергеевна\",\n" +
                "          \"middle_name_one\": \"С\",\n" +
                "          \"fio\": \"Минская Екатерина Сергеевна\",\n" +
                "          \"department_fio\": \"Минская Екатерина Сергеевна (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"173da7a5-635b-42d0-807f-58292941979f\": {\n" +
                "          \"id\": \"173da7a5-635b-42d0-807f-58292941979f\",\n" +
                "          \"last_name\": \"Манахов\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Андрей\",\n" +
                "          \"middle_name\": \"Дмитриевич\",\n" +
                "          \"middle_name_one\": \"Д\",\n" +
                "          \"fio\": \"Манахов Андрей Дмитриевич\",\n" +
                "          \"department_fio\": \"Манахов Андрей Дмитриевич (Направление \\\"Генетика\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генетика\\\"\"\n" +
                "        },\n" +
                "        \"9a75161c-68b2-4431-826e-f1b7d92b631f\": {\n" +
                "          \"id\": \"9a75161c-68b2-4431-826e-f1b7d92b631f\",\n" +
                "          \"last_name\": \"Месонжник\",\n" +
                "          \"first_name_one\": \"Н\",\n" +
                "          \"first_name\": \"Наталья\",\n" +
                "          \"middle_name\": \"Владимировна\",\n" +
                "          \"middle_name_one\": \"В\",\n" +
                "          \"fio\": \"Месонжник Наталья Владимировна\",\n" +
                "          \"department_fio\": \"Месонжник Наталья Владимировна (Ресурсный центр аналитических методов)\",\n" +
                "          \"department\": \"Ресурсный центр аналитических методов\"\n" +
                "        },\n" +
                "        \"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\": {\n" +
                "          \"id\": \"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\",\n" +
                "          \"last_name\": \"Якшин\",\n" +
                "          \"first_name_one\": \"Д\",\n" +
                "          \"first_name\": \"Дмитрий\",\n" +
                "          \"middle_name\": \"Михайлович\",\n" +
                "          \"middle_name_one\": \"М\",\n" +
                "          \"fio\": \"Якшин Дмитрий Михайлович\",\n" +
                "          \"department_fio\": \"Якшин Дмитрий Михайлович (Ресурсный центр генетической инженерии)\",\n" +
                "          \"department\": \"Ресурсный центр генетической инженерии\"\n" +
                "        },\n" +
                "        \"6a6578d5-b080-4bce-8366-b0d01e893a61\": {\n" +
                "          \"id\": \"6a6578d5-b080-4bce-8366-b0d01e893a61\",\n" +
                "          \"last_name\": \"Колесова\",\n" +
                "          \"first_name_one\": \"Е\",\n" +
                "          \"first_name\": \"Екатерина\",\n" +
                "          \"middle_name\": \"Петровна\",\n" +
                "          \"middle_name_one\": \"П\",\n" +
                "          \"fio\": \"Колесова Екатерина Петровна\",\n" +
                "          \"department_fio\": \"Колесова Екатерина Петровна (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"a37afafc-4d8f-4fa9-9918-72105e73cac7\": {\n" +
                "          \"id\": \"a37afafc-4d8f-4fa9-9918-72105e73cac7\",\n" +
                "          \"last_name\": \"Галиева\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Алима\",\n" +
                "          \"middle_name\": \"Абдураимовна\",\n" +
                "          \"middle_name_one\": \"А\",\n" +
                "          \"fio\": \"Галиева Алима Абдураимовна\",\n" +
                "          \"department_fio\": \"Галиева Алима Абдураимовна (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"2a954b7c-a43e-4c45-8a74-1b4ee0aeff0f\": {\n" +
                "          \"id\": \"2a954b7c-a43e-4c45-8a74-1b4ee0aeff0f\",\n" +
                "          \"last_name\": \"Сырочева\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Анастасия\",\n" +
                "          \"middle_name\": \"Олеговна\",\n" +
                "          \"middle_name_one\": \"О\",\n" +
                "          \"fio\": \"Сырочева Анастасия Олеговна\",\n" +
                "          \"department_fio\": \"Сырочева Анастасия Олеговна (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"7693befb-2964-4d99-8b6a-1976c65b1a02\": {\n" +
                "          \"id\": \"7693befb-2964-4d99-8b6a-1976c65b1a02\",\n" +
                "          \"last_name\": \"Афонин\",\n" +
                "          \"first_name_one\": \"М\",\n" +
                "          \"first_name\": \"Михаил\",\n" +
                "          \"middle_name\": \"Борисович\",\n" +
                "          \"middle_name_one\": \"Б\",\n" +
                "          \"fio\": \"Афонин Михаил Борисович\",\n" +
                "          \"department_fio\": \"Афонин Михаил Борисович (Ресурсный центр аналитических методов)\",\n" +
                "          \"department\": \"Ресурсный центр аналитических методов\"\n" +
                "        },\n" +
                "        \"0b25137a-0df6-43b8-a0c0-600464466f82\": {\n" +
                "          \"id\": \"0b25137a-0df6-43b8-a0c0-600464466f82\",\n" +
                "          \"last_name\": \"Чувашов\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Антон\",\n" +
                "          \"middle_name\": \"Андреевич\",\n" +
                "          \"middle_name_one\": \"А\",\n" +
                "          \"fio\": \"Чувашов Антон Андреевич\",\n" +
                "          \"department_fio\": \"Чувашов Антон Андреевич (Лабораторный комплекс)\",\n" +
                "          \"department\": \"Лабораторный комплекс\"\n" +
                "        },\n" +
                "        \"63b50441-3317-4bd8-bfb5-1e04ca0b57e7\": {\n" +
                "          \"id\": \"63b50441-3317-4bd8-bfb5-1e04ca0b57e7\",\n" +
                "          \"last_name\": \"Кульдюшев\",\n" +
                "          \"first_name_one\": \"Н\",\n" +
                "          \"first_name\": \"Никита\",\n" +
                "          \"middle_name\": \"Александрович\",\n" +
                "          \"middle_name_one\": \"А\",\n" +
                "          \"fio\": \"Кульдюшев Никита Александрович\",\n" +
                "          \"department_fio\": \"Кульдюшев Никита Александрович (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"e5b0264b-cf62-4dba-bae0-793febf78529\": {\n" +
                "          \"id\": \"e5b0264b-cf62-4dba-bae0-793febf78529\",\n" +
                "          \"last_name\": \"Бровин\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Андрей\",\n" +
                "          \"middle_name\": \"Николаевич\",\n" +
                "          \"middle_name_one\": \"Н\",\n" +
                "          \"fio\": \"Бровин Андрей Николаевич\",\n" +
                "          \"department_fio\": \"Бровин Андрей Николаевич (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"2a6ceeb5-429d-4fc1-8238-3dbd6c3598a2\": {\n" +
                "          \"id\": \"2a6ceeb5-429d-4fc1-8238-3dbd6c3598a2\",\n" +
                "          \"last_name\": \"Егоров\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Александр\",\n" +
                "          \"middle_name\": \"Дмитриевич\",\n" +
                "          \"middle_name_one\": \"Д\",\n" +
                "          \"fio\": \"Егоров Александр Дмитриевич\",\n" +
                "          \"department_fio\": \"Егоров Александр Дмитриевич (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"760a5cea-fbda-438d-8efa-b20043d432fd\": {\n" +
                "          \"id\": \"760a5cea-fbda-438d-8efa-b20043d432fd\",\n" +
                "          \"last_name\": \"Розанов\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Алексей\",\n" +
                "          \"middle_name\": \"Сергеевич\",\n" +
                "          \"middle_name_one\": \"С\",\n" +
                "          \"fio\": \"Розанов Алексей Сергеевич\",\n" +
                "          \"department_fio\": \"Розанов Алексей Сергеевич (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"91e5b8aa-031d-4ebc-bbab-c300c32e461c\": {\n" +
                "          \"id\": \"91e5b8aa-031d-4ebc-bbab-c300c32e461c\",\n" +
                "          \"last_name\": \"Лапшин\",\n" +
                "          \"first_name_one\": \"Е\",\n" +
                "          \"first_name\": \"Евгений\",\n" +
                "          \"middle_name\": \"Витальевич\",\n" +
                "          \"middle_name_one\": \"В\",\n" +
                "          \"fio\": \"Лапшин Евгений Витальевич\",\n" +
                "          \"department_fio\": \"Лапшин Евгений Витальевич (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"6c159739-052f-412f-a2be-da2709c7924b\": {\n" +
                "          \"id\": \"6c159739-052f-412f-a2be-da2709c7924b\",\n" +
                "          \"last_name\": \"Дахневич\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Анастасия\",\n" +
                "          \"middle_name\": \"Ярославовна\",\n" +
                "          \"middle_name_one\": \"Я\",\n" +
                "          \"fio\": \"Дахневич Анастасия Ярославовна\",\n" +
                "          \"department_fio\": \"Дахневич Анастасия Ярославовна (Направление \\\"Медицинская биотехнология\\\")\",\n" +
                "          \"department\": \"Направление \\\"Медицинская биотехнология\\\"\"\n" +
                "        },\n" +
                "        \"821b1ffc-b389-4513-bd59-1f258288cf7e\": {\n" +
                "          \"id\": \"821b1ffc-b389-4513-bd59-1f258288cf7e\",\n" +
                "          \"last_name\": \"Чувпило\",\n" +
                "          \"first_name_one\": \"С\",\n" +
                "          \"first_name\": \"Сергей\",\n" +
                "          \"middle_name\": \"Альбертович\",\n" +
                "          \"middle_name_one\": \"А\",\n" +
                "          \"fio\": \"Чувпило Сергей Альбертович\",\n" +
                "          \"department_fio\": \"Чувпило Сергей Альбертович (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        },\n" +
                "        \"bbdfb9b5-c170-4941-87ce-c8a6ba4fd4ca\": {\n" +
                "          \"id\": \"bbdfb9b5-c170-4941-87ce-c8a6ba4fd4ca\",\n" +
                "          \"last_name\": \"Ряполова\",\n" +
                "          \"first_name_one\": \"А\",\n" +
                "          \"first_name\": \"Анастасия\",\n" +
                "          \"middle_name\": \"Владимировна\",\n" +
                "          \"middle_name_one\": \"В\",\n" +
                "          \"fio\": \"Ряполова Анастасия Владимировна\",\n" +
                "          \"department_fio\": \"Ряполова Анастасия Владимировна (Направление \\\"Генная терапия\\\")\",\n" +
                "          \"department\": \"Направление \\\"Генная терапия\\\"\"\n" +
                "        }\n" +
                "      }}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(teachers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode teachersNode = root.get("teachersList");
        if (teachersNode != null) {
            Iterator<Map.Entry<String, JsonNode>> teachersIterator = teachersNode.fields();
            while (teachersIterator.hasNext()) {
                Map.Entry<String, JsonNode> teacherEntry = teachersIterator.next();
                String teacherId = teacherEntry.getKey();
                JsonNode teacherNode = teacherEntry.getValue();

                String lastName = teacherNode.get("last_name").asText();
                String firstName = teacherNode.get("first_name").asText();
                String middleName = teacherNode.get("middle_name").asText();
                String department = teacherNode.get("department").asText();

                log.info(teacherId);
                System.out.println("Teacher ID: " + teacherId);
                System.out.println("Last Name: " + lastName);
                System.out.println("First Name: " + firstName);
                System.out.println("Middle Name: " + middleName);
                System.out.println("Department: " + department);
                System.out.println();
            }
        }

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
                .search(search)
                .teacherName("")
                .teacher("")
                .teacherShow(true)
                .teachersList(gson.toJson(teachers))
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

        DataMetaForTeacher dataMetaForGroup = DataMetaForTeacher.builder()
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

}
