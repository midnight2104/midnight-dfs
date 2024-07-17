package com.midnight.dfs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import static com.midnight.dfs.FileUtils.getMimeType;
import static com.midnight.dfs.FileUtils.getUUIDFile;
import static com.midnight.dfs.HttpSyncer.XFILENAME;

@RestController
@Slf4j
public class FileController {

    @Value("${mdfs.path}")
    private String uploadPath;

    @Value("${mdfs.isSync}")
    private boolean isSync;

    @Value("${mdfs.backupUrl}")
    private String backupUrl;

    @Autowired
    HttpSyncer syncer;

    @Value("${mdfs.autoMd5}")
    private boolean autoMd5;


    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file, HttpServletRequest request) {
        String filename = request.getHeader(XFILENAME);
        boolean sync = false;
        if (filename == null || filename.isEmpty()) {
            filename = getUUIDFile(file.getOriginalFilename());
            sync = true;
        }

        String subdir = FileUtils.getSubdir(filename);

        String path = uploadPath + "/" + subdir + "/" + filename;
        log.info("====> path " + path);
        File dest = new File(path);
        file.transferTo(dest);

        // 2. 处理meta
        FileMeta meta = new FileMeta();
        meta.setName(filename);
        meta.setOriginalFileName(file.getOriginalFilename());
        meta.setSize(file.getSize());
        if (autoMd5) {
            meta.getTags().put("md5", DigestUtils.md5DigestAsHex(new FileInputStream(dest)));
        }

        // 2.1 存放到本地文件
        String metaName = filename + ".meta";
        File metaFile = new File(uploadPath + "/" + subdir + "/" + metaName);
        FileUtils.writeMeta(metaFile, meta);

        // 2.2 存到数据库
        // 2.3 存放到配置中心或注册中心，比如zk

        // 3. 同步到backup
        // 同步文件到backup
        if (sync) {
            syncer.sync(dest, backupUrl);
        }

        return filename;
    }

    @PostMapping("/download")
    public void download(String name, HttpServletResponse response) throws IOException {
        String subdir = FileUtils.getSubdir(name);
        String path = uploadPath + "/" + subdir + "/" + name;
        File file = new File(path);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStream fis = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[16 * 1024];

            // 加一些response的头
            response.setCharacterEncoding("UTF-8");
            // response.setContentType("application/octet-stream");
            response.setContentType(getMimeType(name));
            // response.setHeader("Content-Disposition", "attachment;filename=" + name);
            response.setHeader("Content-Length", String.valueOf(file.length()));

            // 读取文件信息，并逐段输出
            OutputStream outputStream = response.getOutputStream();
            while (fis.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            outputStream.flush();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
