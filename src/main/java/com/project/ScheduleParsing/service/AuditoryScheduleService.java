package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.dto.Day;
import com.project.ScheduleParsing.dto.Pair;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.exception.ScheduleNotFoundException;
import com.project.ScheduleParsing.request.RequestGroup;
import com.project.ScheduleParsing.request.servermemo.MonthForFront;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoryScheduleService {

    private String liveWireToken;

    private String xsrfToken;

    private String siriusSession;

    private String wireId;

    private String htmlHash;

    private String checkSum;

    private Integer numWeek;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public Schedule getScheduleByAuditory(String auditory, int week) {
        log.info("AuditoryScheduleService: start getScheduleByAuditory(): {}, {}", auditory, week);
        try {
            RequestGroup requestGroup1 = firstConnectionToSchedule();
            secondConnectionToSchedule(requestGroup1);
            return thirdConnectionToSchedule(auditory, week);
        } catch (IOException e) {
            throw new ScheduleNotFoundException(e.getMessage());
        }
    }

    public RequestGroup firstConnectionToSchedule() throws IOException {
        log.info("AuditoryScheduleService: start firstConnectionToSchedule()");
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

        return null;
    }

    public void secondConnectionToSchedule(RequestGroup firstRequestGroup) throws IOException {
        log.info("AuditoryScheduleService: start secondConnectionToSchedule()");

        String test = "{\"fingerprint\":{\"id\":\"7uQ6Fq0xsgBwqqNy6plx\",\"name\":\"teachers.teacher-main-grid\",\"locale\":\"ru\",\"path\":\"teacher\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"2d6f408e\",\"data\":{\"year\":\"2024\",\"date\":\"09.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":7,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"gridRoute\":\"schedule.teachers.grid\",\"listRoute\":\"schedule.teachers.main\",\"search\":null,\"teacherName\":\"\",\"teacher\":\"\",\"teacherShow\":true,\"teachersList\":{\"Бородин П.М.\":\"Бородин П.М.\",\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\":{\"id\":\"1ac9b76c-2c7a-46fa-a555-a49226d7c58b\",\"last_name\":\"Карабельский\",\"first_name_one\":\"А\",\"first_name\":\"Александр\",\"middle_name\":\"Владимирович\",\"middle_name_one\":\"В\",\"fio\":\"Карабельский Александр Владимирович\",\"department_fio\":\"Карабельский Александр Владимирович (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"cefddf57-4108-4bfa-8f78-e43dd8639b1c\":{\"id\":\"cefddf57-4108-4bfa-8f78-e43dd8639b1c\",\"last_name\":\"Чечушков\",\"first_name_one\":\"А\",\"first_name\":\"Антон\",\"middle_name\":\"Владимирович\",\"middle_name_one\":\"В\",\"fio\":\"Чечушков Антон Владимирович\",\"department_fio\":\"Чечушков Антон Владимирович (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"9543d3e9-6e54-42b8-879c-98d73a6ba63e\":{\"id\":\"9543d3e9-6e54-42b8-879c-98d73a6ba63e\",\"last_name\":\"Минская\",\"first_name_one\":\"Е\",\"first_name\":\"Екатерина\",\"middle_name\":\"Сергеевна\",\"middle_name_one\":\"С\",\"fio\":\"Минская Екатерина Сергеевна\",\"department_fio\":\"Минская Екатерина Сергеевна (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"173da7a5-635b-42d0-807f-58292941979f\":{\"id\":\"173da7a5-635b-42d0-807f-58292941979f\",\"last_name\":\"Манахов\",\"first_name_one\":\"А\",\"first_name\":\"Андрей\",\"middle_name\":\"Дмитриевич\",\"middle_name_one\":\"Д\",\"fio\":\"Манахов Андрей Дмитриевич\",\"department_fio\":\"Манахов Андрей Дмитриевич (Направление \\\"Генетика\\\")\",\"department\":\"Направление \\\"Генетика\\\"\"},\"9a75161c-68b2-4431-826e-f1b7d92b631f\":{\"id\":\"9a75161c-68b2-4431-826e-f1b7d92b631f\",\"last_name\":\"Месонжник\",\"first_name_one\":\"Н\",\"first_name\":\"Наталья\",\"middle_name\":\"Владимировна\",\"middle_name_one\":\"В\",\"fio\":\"Месонжник Наталья Владимировна\",\"department_fio\":\"Месонжник Наталья Владимировна (Ресурсный центр аналитических методов)\",\"department\":\"Ресурсный центр аналитических методов\"},\"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\":{\"id\":\"7fa3d2ba-f02f-4aef-98ea-ae385c7a8453\",\"last_name\":\"Якшин\",\"first_name_one\":\"Д\",\"first_name\":\"Дмитрий\",\"middle_name\":\"Михайлович\",\"middle_name_one\":\"М\",\"fio\":\"Якшин Дмитрий Михайлович\",\"department_fio\":\"Якшин Дмитрий Михайлович (Ресурсный центр генетической инженерии)\",\"department\":\"Ресурсный центр генетической инженерии\"},\"6a6578d5-b080-4bce-8366-b0d01e893a61\":{\"id\":\"6a6578d5-b080-4bce-8366-b0d01e893a61\",\"last_name\":\"Колесова\",\"first_name_one\":\"Е\",\"first_name\":\"Екатерина\",\"middle_name\":\"Петровна\",\"middle_name_one\":\"П\",\"fio\":\"Колесова Екатерина Петровна\",\"department_fio\":\"Колесова Екатерина Петровна (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"a37afafc-4d8f-4fa9-9918-72105e73cac7\":{\"id\":\"a37afafc-4d8f-4fa9-9918-72105e73cac7\",\"last_name\":\"Галиева\",\"first_name_one\":\"А\",\"first_name\":\"Алима\",\"middle_name\":\"Абдураимовна\",\"middle_name_one\":\"А\",\"fio\":\"Галиева Алима Абдураимовна\",\"department_fio\":\"Галиева Алима Абдураимовна (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"2a954b7c-a43e-4c45-8a74-1b4ee0aeff0f\":{\"id\":\"2a954b7c-a43e-4c45-8a74-1b4ee0aeff0f\",\"last_name\":\"Сырочева\",\"first_name_one\":\"А\",\"first_name\":\"Анастасия\",\"middle_name\":\"Олеговна\",\"middle_name_one\":\"О\",\"fio\":\"Сырочева Анастасия Олеговна\",\"department_fio\":\"Сырочева Анастасия Олеговна (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"7693befb-2964-4d99-8b6a-1976c65b1a02\":{\"id\":\"7693befb-2964-4d99-8b6a-1976c65b1a02\",\"last_name\":\"Афонин\",\"first_name_one\":\"М\",\"first_name\":\"Михаил\",\"middle_name\":\"Борисович\",\"middle_name_one\":\"Б\",\"fio\":\"Афонин Михаил Борисович\",\"department_fio\":\"Афонин Михаил Борисович (Ресурсный центр аналитических методов)\",\"department\":\"Ресурсный центр аналитических методов\"},\"0b25137a-0df6-43b8-a0c0-600464466f82\":{\"id\":\"0b25137a-0df6-43b8-a0c0-600464466f82\",\"last_name\":\"Чувашов\",\"first_name_one\":\"А\",\"first_name\":\"Антон\",\"middle_name\":\"Андреевич\",\"middle_name_one\":\"А\",\"fio\":\"Чувашов Антон Андреевич\",\"department_fio\":\"Чувашов Антон Андреевич (Лабораторный комплекс)\",\"department\":\"Лабораторный комплекс\"},\"63b50441-3317-4bd8-bfb5-1e04ca0b57e7\":{\"id\":\"63b50441-3317-4bd8-bfb5-1e04ca0b57e7\",\"last_name\":\"Кульдюшев\",\"first_name_one\":\"Н\",\"first_name\":\"Никита\",\"middle_name\":\"Александрович\",\"middle_name_one\":\"А\",\"fio\":\"Кульдюшев Никита Александрович\",\"department_fio\":\"Кульдюшев Никита Александрович (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"e5b0264b-cf62-4dba-bae0-793febf78529\":{\"id\":\"e5b0264b-cf62-4dba-bae0-793febf78529\",\"last_name\":\"Бровин\",\"first_name_one\":\"А\",\"first_name\":\"Андрей\",\"middle_name\":\"Николаевич\",\"middle_name_one\":\"Н\",\"fio\":\"Бровин Андрей Николаевич\",\"department_fio\":\"Бровин Андрей Николаевич (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"2a6ceeb5-429d-4fc1-8238-3dbd6c3598a2\":{\"id\":\"2a6ceeb5-429d-4fc1-8238-3dbd6c3598a2\",\"last_name\":\"Егоров\",\"first_name_one\":\"А\",\"first_name\":\"Александр\",\"middle_name\":\"Дмитриевич\",\"middle_name_one\":\"Д\",\"fio\":\"Егоров Александр Дмитриевич\",\"department_fio\":\"Егоров Александр Дмитриевич (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"760a5cea-fbda-438d-8efa-b20043d432fd\":{\"id\":\"760a5cea-fbda-438d-8efa-b20043d432fd\",\"last_name\":\"Розанов\",\"first_name_one\":\"А\",\"first_name\":\"Алексей\",\"middle_name\":\"Сергеевич\",\"middle_name_one\":\"С\",\"fio\":\"Розанов Алексей Сергеевич\",\"department_fio\":\"Розанов Алексей Сергеевич (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"91e5b8aa-031d-4ebc-bbab-c300c32e461c\":{\"id\":\"91e5b8aa-031d-4ebc-bbab-c300c32e461c\",\"last_name\":\"Лапшин\",\"first_name_one\":\"Е\",\"first_name\":\"Евгений\",\"middle_name\":\"Витальевич\",\"middle_name_one\":\"В\",\"fio\":\"Лапшин Евгений Витальевич\",\"department_fio\":\"Лапшин Евгений Витальевич (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"6c159739-052f-412f-a2be-da2709c7924b\":{\"id\":\"6c159739-052f-412f-a2be-da2709c7924b\",\"last_name\":\"Дахневич\",\"first_name_one\":\"А\",\"first_name\":\"Анастасия\",\"middle_name\":\"Ярославовна\",\"middle_name_one\":\"Я\",\"fio\":\"Дахневич Анастасия Ярославовна\",\"department_fio\":\"Дахневич Анастасия Ярославовна (Направление \\\"Медицинская биотехнология\\\")\",\"department\":\"Направление \\\"Медицинская биотехнология\\\"\"},\"821b1ffc-b389-4513-bd59-1f258288cf7e\":{\"id\":\"821b1ffc-b389-4513-bd59-1f258288cf7e\",\"last_name\":\"Чувпило\",\"first_name_one\":\"С\",\"first_name\":\"Сергей\",\"middle_name\":\"Альбертович\",\"middle_name_one\":\"А\",\"fio\":\"Чувпило Сергей Альбертович\",\"department_fio\":\"Чувпило Сергей Альбертович (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"},\"bbdfb9b5-c170-4941-87ce-c8a6ba4fd4ca\":{\"id\":\"bbdfb9b5-c170-4941-87ce-c8a6ba4fd4ca\",\"last_name\":\"Ряполова\",\"first_name_one\":\"А\",\"first_name\":\"Анастасия\",\"middle_name\":\"Владимировна\",\"middle_name_one\":\"В\",\"fio\":\"Ряполова Анастасия Владимировна\",\"department_fio\":\"Ряполова Анастасия Владимировна (Направление \\\"Генная терапия\\\")\",\"department\":\"Направление \\\"Генная терапия\\\"\"}},\"currentRouteName\":\"teacher.grid\",\"watching\":false,\"events\":[],\"eventElement\":[],\"statusInit\":true,\"lectures\":true,\"seminars\":true,\"practices\":true,\"laboratories\":true,\"exams\":true,\"other\":true,\"width\":null,\"height\":null},\"dataMeta\":{\"collections\":[\"teachersList\"]},\"checksum\":\"612d2bf9378255b6c219e2f340b9635fbcc159f1071babad9afea68d1c332132\"},\"updates\":[{\"type\":\"callMethod\",\"payload\":{\"id\":\"hoya\",\"method\":\"render\",\"params\":[]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"v46c\",\"method\":\"$set\",\"params\":[\"width\",630]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"ht14w12l\",\"method\":\"$set\",\"params\":[\"height\",705]}}]}";

        Connection.Response response = getConnection(test);
    }

    public Schedule thirdConnectionToSchedule(String auditoryName, int week) throws IOException {
        log.info("AuditoryScheduleService: start thirdConnectionToSchedule(): {}, {}", auditoryName, week);

        String staticAud = "{\"fingerprint\":{\"id\":\"q79h5tgHhBK9DRNTkTqU\",\"name\":\"classroom.classroom-main-grid\",\"locale\":\"ru\",\"path\":\"classroom\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"618285d0\",\"data\":{\"year\":\"2024\",\"date\":\"15.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":7,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"gridRoute\":\"schedule.classroom.grid\",\"listRoute\":\"schedule.classroom.main\",\"search\":\"л\",\"classroom\":\"\",\"classroomsList\":{\"0\":\"М_Л. Конференц-зал Достоевский (Университет)\",\"1\":\"ЛК\",\"2\":\"Альфа 5.11 (Основной)\",\"3\":\"Альфа 5.2 (Основной)\",\"4\":\"ЛК РЦ ГЦ\",\"5\":\"Альфа 5.5 (Основной)\",\"7\":\"Коворкинг 3,54, ЛК пом.В\",\"8\":\"Альфа 5.10 (Основной)\",\"10\":\"Альфа 5.8 (Основной)\",\"11\":\"Зал «Атом»\",\"17\":\"Тема: Как строится работа над ИТ продуктами в больших компаниях\",\"23\":\"Тема: Об управлении командой и конфликтами, навык презентации идей , переговоры\",\"24\":\"Зал Атом (Основной)\",\"27\":\"Конференц-зал Толстой (Основной)\",\"28\":\"К_Ростелеком (Основной)\",\"32\":\"К_0 (стартап-лаборатория ИНТЦ) (Основной)\",\"33\":\"Альфа 4.3 (Основной)\",\"34\":\"Альфа 4.4 (Основной)\",\"35\":\"К_0 (Лингафонный кабинет) (Основной)\",\"36\":\"Альфа 4.2 (Основной)\"},\"currentRouteName\":\"classroom.grid\",\"watching\":false,\"events\":[],\"eventElement\":[],\"statusInit\":true,\"lectures\":true,\"seminars\":true,\"practices\":true,\"laboratories\":true,\"exams\":true,\"other\":true,\"width\":630,\"height\":705},\"dataMeta\":{\"collections\":[\"classroomsList\"]},\"checksum\":\"0404be368d29891341cae9d62729d3ee2fb99bf10ba85738a691f4f018dc74ee\"},\"updates\":[{\"type\":\"callMethod\",\"payload\":{\"id\":\"qiu4\",\"method\":\"set\",\"params\":[\"AuditoryName\"]}}";
        StringBuilder sb = new StringBuilder(staticAud.replace("AuditoryName", auditoryName));

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

        return parseSchedule(htmlSchedule);
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

    private String extractValueFromHtml(String html, String key) {
        String regex = key + "&quot;:&quot;([^&quot;]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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
