package com.project.ScheduleParsing.request.servermemo;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupList {

    @SerializedName("class")
    private String className;
    private List<Object> id;
    private List<Object> relations;
    private String connection;
}
