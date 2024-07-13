package com.midnight.dfs.syncer;

import java.io.File;

public interface FileSyncer {
    boolean sync(   File file, String backUrl, boolean  sync);
}
