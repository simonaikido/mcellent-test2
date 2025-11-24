package com.bim.seif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages =  "com.bim.seif.clients")
@SpringBootApplication(scanBasePackages = "com.bim.seif")
public class SiefApplication {

	public static void main(String[] args) {
		SpringApplication.run(SiefApplication.class, args);
	}

}
