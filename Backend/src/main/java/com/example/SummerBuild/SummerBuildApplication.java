package com.example.SummerBuild;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class SummerBuildApplication {

	public static void main(String[] args) {
		SpringApplication.run(SummerBuildApplication.class, args);
	}

}
