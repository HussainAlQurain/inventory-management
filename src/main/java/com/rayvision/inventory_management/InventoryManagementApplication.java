package com.rayvision.inventory_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.logging.Logger;

@SpringBootApplication
@EnableScheduling
public class InventoryManagementApplication implements CommandLineRunner {

	private static final Logger LOGGER = Logger.getLogger(InventoryManagementApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(InventoryManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

	}
}
