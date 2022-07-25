package org.litesoft.wip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	public static final String VERSION = "0.1";

	public static void main(String[] args) {
		System.out.println(ATTENTION + "WIP application vs " + VERSION + ATTENTION);
		SpringApplication.run( Application.class, args);
	}

	private static final String USER_DIR = System.getProperty( "user.dir" );
	private static final String ATTENTION = " ******************************************** ";
}
