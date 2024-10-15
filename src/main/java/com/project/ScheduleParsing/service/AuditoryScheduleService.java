package com.project.ScheduleParsing.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.dto.Schedule;
import com.project.ScheduleParsing.request.Request;
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
import java.util.Map;
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

    public Schedule getScheduleByAuditory(String auditory, Integer week) {

        try {
            Request request1 = firstConnectionToSchedule();
            secondConnectionToSchedule(request1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Request firstConnectionToSchedule() throws IOException {
        log.info("GroupScheduleService: firstConnectionToSchedule()");
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

    public Request secondConnectionToSchedule(Request firstRequest) throws IOException {
        log.info("GroupScheduleService: secondConnectionToSchedule()");

        Connection.Response response = getConnection(firstRequest);
        String decodedString = StringEscapeUtils.unescapeJava(response.body());
        htmlHash = extractValueFromJson(decodedString, "htmlHash");
        checkSum = extractValueFromJson(decodedString, "checksum");
        log.info(response.body());
        return null;
    }

    private Connection.Response getConnection(Request request) throws IOException {

        String t = "{\"fingerprint\":{\"id\":\"nynBJegwU9K4Ak5Us5t7\",\"name\":\"classroom.classroom-main-grid\",\"locale\":\"ru\",\"path\":\"classroom\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"2615a6d5\",\"data\":{\"year\":\"2024\",\"date\":\"14.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":7,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"search\":\"\",\"group\":\"\",\"groupList\":[],\"events\":[],\"eventElement\":[],\"watching\":false,\"currentRouteName\":\"grid\",\"statusInit\":false,\"lectures\":true,\"seminars\":true,\"practices\":true,\"laboratories\":true,\"exams\":true,\"other\":true,\"width\":null,\"height\":null},\"dataMeta\":{\"modelCollections\":{\"groupList\":{\"class\":\"App\\\\Models\\\\Group\",\"id\":[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null],\"relations\":[],\"connection\":\"mysql\"}}},\"checksum\":\"f616db0d18d70f8d6e784ae3bf2c0af2748600741172078cbfe6b71ba02b18c5\"},\"updates\":[{\"type\":\"callMethod\",\"payload\":{\"id\":\"w887\",\"method\":\"render\",\"params\":[]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"rx9e\",\"method\":\"$set\",\"params\":[\"width\",630]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"ath1\",\"method\":\"$set\",\"params\":[\"height\",705]}}]}";
        String json = "{\"fingerprint\":{\"id\":\""+ wireId + "\",\"name\":\"classroom.classroom-main-grid\",\"locale\":\"ru\",\"path\":\"classroom\",\"method\":\"GET\",\"v\":\"acj\"},\"serverMemo\":{\"children\":[],\"errors\":[],\"htmlHash\":\"" + htmlHash + "\",\"data\":{\"year\":\"2024\",\"date\":\"14.10.2024\",\"month\":{\"number\":\"10\",\"full\":\"Октябрь\",\"fullForDisplay\":\"Октября\"},\"numWeek\":7,\"addNumWeek\":0,\"minusNumWeek\":0,\"count\":0,\"type\":\"grid\",\"gridRoute\":\"schedule.classroom.grid\",\"listRoute\":\"schedule.classroom.main\",\"search\":null,\"classroom\":\"\",\"classroomsList\":[\"М_Л. Конференц-зал Достоевский (Университет)\",\"ЛК\",\"Альфа 5.11 (Основной)\",\"Альфа 5.2 (Основной)\",\"ЛК РЦ ГЦ\",\"Альфа 5.5 (Основной)\",\"Коворкинг 3,54\",\"Коворкинг 3,54, ЛК пом.В\",\"Альфа 5.10 (Основной)\",\"Альфа 5.8 (Основной)\",\"Зал «Атом»\",\"К_19 (Основной)\",\"К_4 (Основной)\",\"К_6 (Основной)\",\"К_2 (Основной)\",\"К_9 (Основной)\",\"Тема: Как строится работа над ИТ продуктами в больших компаниях\",\"Тема: UX/UI дизайн\",\"Тема: Java backend разработка\",\"Тема: Systems analisys\"],\"currentRouteName\":\"classroom.grid\",\"watching\":false,\"events\":[],\"eventElement\":[],\"statusInit\":true,\"lectures\":true,\"seminars\":true,\"practices\":true,\"laboratories\":true,\"exams\":true,\"other\":true,\"width\":null,\"height\":null},\"dataMeta\":{\"collections\":[\"classroomsList\"]},\"checksum\":\"" + checkSum + "\"},\"updates\":[{\"type\":\"callMethod\",\"payload\":{\"id\":\"wiz4\",\"method\":\"render\",\"params\":[]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"eww3\",\"method\":\"$set\",\"params\":[\"width\",630]}},{\"type\":\"callMethod\",\"payload\":{\"id\":\"ut0a\",\"method\":\"$set\",\"params\":[\"height\",705]}}]}";

        return Jsoup.connect("https://schedule.siriusuniversity.ru/livewire/message/classroom.classroom-main-grid")
                .header("Content-Length", String.valueOf(1184))
                .header("Content-Type", "application/json")
                .header("Cookie", xsrfToken + "; " + siriusSession)
                .header("X-Csrf-Token", liveWireToken)
                .header("X-Livewire", "true")
                .requestBody(json)
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

}
