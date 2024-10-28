package com.project.ScheduleParsing.request.servermemo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataMetaForTeacherAndAuditory {

    private List<String> collections;
}
