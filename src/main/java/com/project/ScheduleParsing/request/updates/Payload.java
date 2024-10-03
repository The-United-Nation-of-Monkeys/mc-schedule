package com.project.ScheduleParsing.request.updates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class Payload {

    private String id;
    private String method;
    private List<Object> params;
}
