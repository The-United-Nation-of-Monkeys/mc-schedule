package com.project.ScheduleParsing.request.updates;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayloadForTeacher {

    private String id;

    private String name;

    private String value;
}
