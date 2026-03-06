package com.workdiary;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.workdiary.mapper")
public class WorkDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkDiaryApplication.class, args);
        System.out.println("== Work Diary Server Started ==");
        System.out.println("== Access Swagger API Docs at: http://localhost:8080/api/doc.html ==");
    }

}
