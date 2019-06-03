package net.cc.luffy.storage.ucare.inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShellTestApplication {

	private final static Logger logger = LoggerFactory.getLogger(ShellTestApplication.class);

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ShellTestApplication.class, args);

	}

}
