package com.project.ScheduleParsing.request;

import com.project.ScheduleParsing.request.servermemo.ServerMemo;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RequestTeacher {

    private Fingerprint fingerprint;
    private Effects effects;
    private ServerMemo serverMemo;
    private List<Update> updates;
}
