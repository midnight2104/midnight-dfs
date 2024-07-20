package com.midnight.dfs.syncer;

import com.alibaba.fastjson.JSON;
import com.midnight.dfs.meta.FileMeta;
import com.midnight.dfs.utils.FileUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;

@Component
public class MqSyncer {
    @Value("${mdfs.group}")
    private String group;

    @Value("${mdfs.path}")
    private String uploadPath;

    @Value("${mdfs.downloadUrl}")
    private String localUrl;

    @Autowired
    private RocketMQTemplate template;

    private String topic = "mdfs";

    public void sync(FileMeta fileMeta) {
        Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(fileMeta)).build();
        template.send(topic, message);
        System.out.println(" ===> send message: " + message);

    }


    @Service
    @RocketMQMessageListener(topic = "mdfs", consumerGroup = "${mdfs.group}")
    public class FileMqSyncer implements RocketMQListener<MessageExt> {

        @Override
        public void onMessage(MessageExt message) {
            // 1. 从消息里拿到meta数据
            System.out.println("===>>>  onMessage id= " + message.getMsgId());
            String json = new String(message.getBody());
            System.out.println("====>>>  message json = " + json);
            FileMeta meta = JSON.parseObject(json, FileMeta.class);
            String downloadUrl = meta.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return;
            }

            // 去重本机操作
            if (localUrl.equals(downloadUrl)) {
                System.out.println(" =====> the same file server, ignore mq sync task.");
                return;
            }

            System.out.println(" ====>>> the other file server, process mq sync task.");

            // 2. 写meta
            String dir = uploadPath + "/" + meta.getName().substring(0, 2);
            File metaFile = new File(dir, meta.getName() + ".meta");
            if (metaFile.exists()) {
                System.out.println(" ==> meta file exists and ignore save: " + metaFile.getAbsolutePath());
            } else {
                System.out.println(" ==> meta file save: " + metaFile.getAbsolutePath());
                FileUtils.writeString(metaFile, json);
            }

            // 3. 下载文件
            File file = new File(dir, meta.getName());
            if (file.exists() && file.length() == meta.getSize()) {
                System.out.println(" ==> file exists and ignore download: " + file.getAbsolutePath());
                return;
            }

            String download = downloadUrl + "?name=" + file.getName();
            FileUtils.download(download, file);
        }
    }
}
