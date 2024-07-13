package com.midnight.dfs;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;

import java.io.File;

@SpringBootApplication
public class MidnightDfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidnightDfsApplication.class, args);
    }

    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setLocation("/private/tmp/tomcat");
        return factory.createMultipartConfig();
    }

    @Value("${mdfs.path}")
    private String uploadPath;

    @Bean
    ApplicationRunner runner() {
        return args -> {
            System.out.println("init mdfs dirs...");
            File path = new File(uploadPath);
            if(!path.exists()) path.mkdirs();
            // 输出256个文件夹，名称为十六进制
            for(int i=0; i<256; i++) {
                String dir = String.format("%02x", i);
                File dirPath = new File(uploadPath, dir);
                if(!dirPath.exists()) dirPath.mkdirs();
            }
        };
    }
}
