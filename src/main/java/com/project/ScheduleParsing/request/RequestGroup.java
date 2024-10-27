package com.project.ScheduleParsing.request;

import com.project.ScheduleParsing.request.servermemo.ServerMemo;
import com.project.ScheduleParsing.request.updates.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestGroup {

    private Fingerprint fingerprint;
    private ServerMemo serverMemo;
    private List<Update> updates;
}
