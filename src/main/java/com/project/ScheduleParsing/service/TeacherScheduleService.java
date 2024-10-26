package com.project.ScheduleParsing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
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
            RequestTeacher requestTeacher2 = secondConnectionToSchedule(teacher, week, request1);

            return finalConnectionToSchedule(requestTeacher2);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new ScheduleNotFoundException(ex.getMessage());
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

    public RequestTeacher secondConnectionToSchedule(String teacherName, int week, String firstRequest) throws IOException {
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

        RequestTeacher secondRequest = buildSecondRequest(teacherName, week, teachersList);
        log.info(gson.toJson(secondRequest));

        return secondRequest;
    }

    public Schedule finalConnectionToSchedule( RequestTeacher firstRequest) throws IOException {
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

        return parseSchedule2(res);

        //return parseSchedule(teacher, html);
    }

    public Schedule parseSchedule2(String json) throws JsonProcessingException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = jsonObject.getJSONObject("serverMemo").getJSONObject("data");
        JSONObject events = data.getJSONObject("events");

        List<Day> days = new ArrayList<>(Collections.nCopies(7, null));
        int allPairCount = data.getInt("count");
        for (String key : events.keySet()) {
            Object eventObject = events.get(key);
            if (eventObject instanceof JSONObject event) {
                Day day = getDayFromJSONObject(event, key);
                days.set(getDayIndex(day.getDayWeek()), day);
            } else {
                JSONArray eventArray = events.getJSONArray(key);
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

        day.setPairList(pairs);
        return day;
    }

    public int getDayIndex(String day) {
        return switch (day.toUpperCase()) {
            case "ПН" -> 0;
            case "ВТ" -> 1;
            case "СР" -> 2;
            case "ЧТ" -> 3;
            case "ПТ" -> 4;
            case "СБ" -> 5;
            case "ВС" -> 6;
            default -> throw new IllegalArgumentException("Некорректный день недели: " + day);
        };
    }





    public Schedule thirdConnectionToSchedule(String teacher, int week) throws IOException {
        log.info("TeacherScheduleService: start thirdConnectionToSchedule(): {}, {}", teacher, week);

        String s = "{\"fingerprint\":{\"id\":\"OCJfQ1wuIX8LrktEfdXe\",\"name\":\"teachers.teacher-main-grid\",\"locale\":\"ru\",\"path\":\"teacher\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"6bff9b76\",\"data\":{\"year\":\"2024\",\"date\":\"15.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":7,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"gridRoute\":\"schedule.teachers.grid\",\"listRoute\":\"schedule.teachers.main\",\"search\":\"Кара\",\"teacherName\":\"\",\"teacher\":\"\",\"teacherShow\":true,\"teachersList\":{\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\":{\"id\":\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\",\"last_name\":\"Карабельский\",\"first_name_one\":\"А\",\"first_name\":\"Александр\",\"middle_name\":\"Владимирович\",\"middle_name_one\":\"В\",\"fio\":\"Карабельский Александр Владимирович\",\"department_fio\":\"Карабельский Александр Владимирович (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"f6b3a33c-0316-41d6-9216-e11cc334f53f\":{\"id\":\"f6b3a33c-0316-41d6-9216-e11cc334f53f\",\"last_name\":\"Каранский\",\"first_name_one\":\"В\",\"first_name\":\"Виталий\",\"middle_name\":\"Владиславович\",\"middle_name_one\":\"В\",\"fio\":\"Каранский Виталий Владиславович\",\"department_fio\":\"Каранский Виталий Владиславович (Колледж Автономной некоммерческой образовательной организации высшего образования «Научно-технологический университет «Сириус»)\",\"department\":\"Колледж Автономной некоммерческой образовательной организации высшего образования «Научно-технологический университет «Сириус»\"},\"41970629-e8d9-4232-95a9-3721bf6749d9\":{\"id\":\"41970629-e8d9-4232-95a9-3721bf6749d9\",\"last_name\":\"Карагозян\",\"first_name_one\":\"Л\",\"first_name\":\"Лиана\",\"middle_name\":\"Диграновна\",\"middle_name_one\":\"Д\",\"fio\":\"Карагозян Лиана Диграновна\",\"department_fio\":\"Карагозян Лиана Диграновна (Президентский Лицей «Сириус»)\",\"department\":\"Президентский Лицей «Сириус»\"},\"3e07ed04-c78f-4f4a-b326-fb9c69094be6\":{\"id\":\"3e07ed04-c78f-4f4a-b326-fb9c69094be6\",\"last_name\":\"Карапетьянц\",\"first_name_one\":\"Н\",\"first_name\":\"Николай\",\"middle_name\":\"\",\"fio\":\"Карапетьянц Николай\",\"department_fio\":\"Карапетьянц Николай (Научный центр информационных технологий и искусственного интеллекта)\",\"department\":\"Научный центр информационных технологий и искусственного интеллекта\"}},\"currentRouteName\":\"teacher.grid\",\"watching\":false,\"events\":[],\"eventElement\":[],\"statusInit\":true,\"lectures\":true,\"seminars\":true,\"practices\":true,\"laboratories\":true,\"exams\":true,\"other\":true,\"width\":630,\"height\":705},\"dataMeta\":{\"collections\":[\"teachersList\"]},\"checksum\":\"9fb23f726ca0b0493fcff10ce8a62da46ee7c39b5c25b159ff6e560426e32bab\"},\"updates\":[{\"type\":\"callMethod\",\"payload\":{\"id\":\"uxcd\",\"method\":\"set\",\"params\":[\"teacherId\"]}}";
        String teacherId = getTeacherId(teacher);
        StringBuilder sb = new StringBuilder(s.replace("teacherId", teacherId));

        if (week == 0) {
            sb.append("]}");
        } else if (week > 0) {
            sb.append(",{\"type\":\"callMethod\",\"payload\":{\"id\":\"lgs4h\",\"method\":\"addWeek\",\"params\":[]}}".repeat(week)).append("]}");
        } else {
            sb.append(",{\"type\":\"callMethod\",\"payload\":{\"id\":\"lgs4h\",\"method\":\"minusWeek\",\"params\":[]}}".repeat(week*(-1))).append("]}");
        }

        Connection.Response response = getConnection(sb.toString());
        String responseBody = response.body();
        String htmlSchedule = StringEscapeUtils.unescapeJava(responseBody);

        String startMarker = "{\"effects\":{\"html\":\"<div wire:id=\"";
        String endMarker = "<div wire:id";
        String endMarker2 = "</div>";

        int startIndex = htmlSchedule.indexOf(startMarker);
        int endIndex = htmlSchedule.indexOf(endMarker, startIndex);
        int endIndex2 = htmlSchedule.lastIndexOf(endMarker2);

        htmlSchedule = htmlSchedule.substring(0, startIndex) + htmlSchedule.substring(endIndex);
        htmlSchedule = htmlSchedule.substring(0, endIndex2-14);

        return parseSchedule(teacher, htmlSchedule);
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

    private RequestTeacher buildSecondRequest(String teacherName, int week, Map<String, Object> teachersList) {
        Fingerprint fingerprint = new Fingerprint(wireId, "teachers.teacher-main-grid", "ru", "teacher", "GET", "acj");
        Effects effects = new Effects();
        ServerMemo serverMemo = createServerMemo(null, true, 630, 705, teachersList);

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

        return new RequestTeacher(fingerprint, effects, serverMemo, updates);
    }

    private ServerMemo createServerMemo(String search, boolean statusInit, Integer width, Integer height, Map<String, Object> teachersList) {
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
                .search(search)
                .teacherName("")
                .teacher("")
                .teacherShow(true)
                .teachersList(teachersList)
                .events(new ArrayList<>())
                .eventElement(new ArrayList<>())
                .watching(false)
                .currentRouteName("teacher.grid")
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

    private Schedule parseSchedule(String teacher, String htmlSchedule) {
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

    private String getTeacherId(String fio) {
        com.project.ScheduleParsing.model.Teacher teacher = teacherRepository.findTeacherByFio(fio);
        return String.valueOf(teacher.getId());
    }

}
