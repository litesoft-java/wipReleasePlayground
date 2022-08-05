package org.litesoft.wip;

import org.litesoft.versioning.Version;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		System.out.println( ATTENTION + "WIP application vs " + Version.getString() + ATTENTION);
		SpringApplication.run( Application.class, args);
	}

	private static final String USER_DIR = System.getProperty( "user.dir" );
	private static final String ATTENTION = " ******************************************** ";
}
