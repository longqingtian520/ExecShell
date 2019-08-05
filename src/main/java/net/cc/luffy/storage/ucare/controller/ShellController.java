package net.cc.luffy.storage.ucare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import net.cc.luffy.storage.ucare.inter.shell.ShellService;

@RestController
public class ShellController {

	@Value("${caac.mailArr:10000}")
	private String caacMail;

	@Autowired
	private ShellService shellService;

	@GetMapping("/mail")
	public String test() {
		System.out.println(caacMail);
		shellService.sendMailToCaac();
		return "success";
	}

}
