package com.project.ScheduleParsing.request.updates;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Update {

    private String type;
    private Payload payload;
}
