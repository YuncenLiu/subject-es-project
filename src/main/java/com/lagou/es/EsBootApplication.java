package com.lagou.es;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lagou.**.mapper")
public class EsBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(EsBootApplication.class,args);
    }
}
