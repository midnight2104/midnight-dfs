package com.midnight.dfs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "mdfs")
@Data
public class MdfsConfigProperties {
    private String uploadPath;
    private String backupUrl;
    private String downloadUrl;
    private String group;
    private boolean autoMd5;
    private boolean syncBackup;
}
