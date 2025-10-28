package com.global.ums;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UmsServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UmsServerApplication.class, args);
	}

}
