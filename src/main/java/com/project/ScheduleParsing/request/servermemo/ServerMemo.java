package com.project.ScheduleParsing.request.servermemo;

import com.google.gson.annotations.SerializedName;
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

    private Object data;

    private Object dataMeta;

    private String checksum;
}
