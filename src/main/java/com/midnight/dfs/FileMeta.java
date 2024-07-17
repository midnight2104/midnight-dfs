package com.midnight.dfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMeta {
    private String name;
    private String originalFileName;
    private long size;
    private Map<String, String> tags = new HashMap<>();
}
