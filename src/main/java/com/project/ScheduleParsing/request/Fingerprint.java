package com.project.ScheduleParsing.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Fingerprint {

    private String id;
    private String name;
    private String locale;
    private String path;
    private String method;
    private String v;
}
