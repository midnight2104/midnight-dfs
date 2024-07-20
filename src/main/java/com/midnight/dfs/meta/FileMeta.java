package com.midnight.dfs.meta;

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
    private String originalFilename;
    private long size;
    private String downloadUrl;
    private Map<String, String> tags = new HashMap<>();

    public FileMeta(String filename, String originalFilename, long size, String downloadUrl) {
        this.name = filename;
        this.originalFilename = originalFilename;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }
}
