package com.project.ScheduleParsing.request.servermemo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServerMemo {

    private List<Object> children;
    private List<Object> errors;
    private String htmlHash;
    private Data data;
    private DataMeta dataMeta;
    private String checksum;
}
