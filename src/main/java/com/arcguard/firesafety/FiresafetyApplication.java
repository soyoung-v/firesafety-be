package com.arcguard.firesafety;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class FiresafetyApplication {

    public static void main(String[] args) {
        // .env는 Java Properties(spring.config.import) 대신 dotenv-java로 직접 읽는다.
        // Properties 규격은 기본이 ISO-8859-1이라 한글 값이 깨지는데, dotenv-java는 UTF-8로 그대로 읽는다.
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(FiresafetyApplication.class, args);
    }

}
