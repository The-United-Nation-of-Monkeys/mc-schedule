package com.project.ScheduleParsing.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Effects {

    private List<Object> listeners = new ArrayList<>();
}
