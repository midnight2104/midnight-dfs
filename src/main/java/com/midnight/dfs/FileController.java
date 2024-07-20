package com.midnight.dfs;

import com.midnight.dfs.config.MdfsConfigProperties;
import com.midnight.dfs.meta.FileMeta;
import com.midnight.dfs.syncer.HttpSyncer;
import com.midnight.dfs.syncer.MqSyncer;
import com.midnight.dfs.utils.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.midnight.dfs.syncer.HttpSyncer.XFILENAME;
import static com.midnight.dfs.utils.FileUtils.getUUIDFile;


@RestController
@Slf4j
public class FileController {

    @Autowired
    MdfsConfigProperties configProperties;

    @Autowired
    HttpSyncer syncer;

    @Autowired
    MqSyncer mqSyncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file, HttpServletRequest request) {
        // 1. 处理文件
        String filename = request.getHeader(XFILENAME);
        boolean sync = false;
        String originalFilename = file.getOriginalFilename();
        // 正常上传
        if (filename == null || filename.isEmpty()) {
            filename = getUUIDFile(file.getOriginalFilename());
            sync = true;
        } else {
            // 同步
            String xor = request.getHeader(HttpSyncer.XORIGFILENAME);
            if (xor != null && !xor.isEmpty()) {
                originalFilename = xor;
            }
        }

        File dest = getFile(FileUtils.getSubdir(filename), filename);
        // 复制文件到指定位置
        file.transferTo(dest);

        // 2.处理meta
        FileMeta fileMeta = new FileMeta(filename, originalFilename, file.getSize(), configProperties.getDownloadUrl());
        if (configProperties.isAutoMd5()) {
            fileMeta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }
        // 存放文件到本地
        FileUtils.writeMeta(new File(dest.getAbsolutePath() + ".meta"), fileMeta);

        // 3. 同步到backup
        if (sync) {
            if (configProperties.isSyncBackup()) {
                try {
                    syncer.sync(dest, configProperties.getBackupUrl(), originalFilename);
                } catch (Exception e) {
                    e.printStackTrace();
                    mqSyncer.sync(fileMeta);
                }
            } else {
                mqSyncer.sync(fileMeta);
            }
        }

        return filename;
    }

    @PostMapping("/download")
    public void download(String name, HttpServletResponse response) throws IOException {
        File file = getFile(FileUtils.getSubdir(name), name);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(FileUtils.getMimeType(name));
        // response.setHeader("Content-Disposition", "attachment;filename=" + name);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        FileUtils.output(file, response.getOutputStream());
    }

    @RequestMapping("/meta")
    public String meta(String name) {
        return FileUtils.readString(getFile(FileUtils.getSubdir(name), name));
    }


    private File getFile(String subdir, String filename) {
        return new File(configProperties.getUploadPath() + "/" + subdir + "/" + filename);
    }

}
