package net.cc.luffy.storage.ucare.inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan({"net.cc.luffy.storage.ucare.controller.**","net.cc.luffy.storage.ucare.inter.shell.**"})
@SpringBootApplication
@EnableScheduling
public class ShellTestApplication {

	private final static Logger logger = LoggerFactory.getLogger(ShellTestApplication.class);

	public static void main(String[] args) throws Exception {
		SpringApplication app = new SpringApplication(ShellTestApplication.class);
		app.addListeners(new ApplicationPidFileWriter());
		app.run(args);

	}

}
