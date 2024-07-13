package com.midnight.dfs;

import com.midnight.dfs.syncer.FileSyncer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

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
    FileSyncer syncer;

    @SneakyThrows
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file, HttpServletRequest request) {
        String name = file.getOriginalFilename();
        String ext = name.substring(name.lastIndexOf("."));

        String filename = request.getHeader("X-Filename");
        boolean sync = false;
        if (filename == null || filename.isEmpty()) {
            filename = UUID.randomUUID() + ext;
            sync = true;
        }

        String dir = filename.substring(0, 2);

        String path = uploadPath + "/" + dir + "/" + filename;
        log.info("====> path " + path);
        File dest = new File(path);
        file.transferTo(dest);

        if (sync) {
            syncer.sync(dest, backupUrl, isSync);
        }

        return filename;
    }

    @PostMapping("/download")
    public void download(String name, HttpServletResponse response) throws IOException {
        String dir = name.substring(0, 2);
        String path = uploadPath + "/" + dir + "/" + name;

        File file = new File(name);
        String filename = file.getName();
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bufferStream = new BufferedInputStream(fis);

        byte[] buffer = new byte[16 * 1024];

        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode(filename, "UTF-8"));
        response.addHeader("Content-Length", "" + file.length());
        response.setContentType("application/octet-stream");

        OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());

        while (bufferStream.read(buffer) > 0) {
            outputStream.write(buffer);
        }

        bufferStream.close();
        outputStream.flush();
    }
}
