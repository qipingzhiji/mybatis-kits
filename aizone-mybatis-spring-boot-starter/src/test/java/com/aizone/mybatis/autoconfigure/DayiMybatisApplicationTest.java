package com.aizone.mybatis.autoconfigure;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author chenzhaoju
 */
@SpringBootApplication
@MapperScan("com.aizone.mybatis.autoconfigure.mapper")
public class DayiMybatisApplicationTest {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(DayiMybatisApplicationTest.class, args);
    }
}
