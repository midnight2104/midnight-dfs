package com.midnight.dfs.syncer;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileSyncerImpl implements FileSyncer {
    private HttpSyncer syncer = new HttpSyncer();

    @Override
    public boolean sync(File file, String backUrl, boolean sync) {
        if (backUrl == null || "null".equals(backUrl)) return false;

        if (sync) {
            syncer.sync(file, backUrl, sync);
        }

        return true;
    }
}
