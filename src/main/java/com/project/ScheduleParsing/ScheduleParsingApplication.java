package com.project.ScheduleParsing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.ScheduleParsing.request.*;
import com.project.ScheduleParsing.request.servermemo.*;
import com.project.ScheduleParsing.request.updates.Payload;
import com.project.ScheduleParsing.request.updates.Update;
import com.project.ScheduleParsing.service.ScheduleParsingService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class ScheduleParsingApplication {

	private static ScheduleParsingService scheduleParsingService = new ScheduleParsingService();

	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		String group = "К0709-23/3";
		Integer week = 0;
		scheduleParsingService.getSchedule(group, week);
		System.out.println(System.currentTimeMillis() - startTime);

		//SpringApplication.run(ScheduleParsingApplication.class, args);
	}

}
