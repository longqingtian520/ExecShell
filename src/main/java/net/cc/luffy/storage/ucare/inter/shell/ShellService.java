package net.cc.luffy.storage.ucare.inter.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.cc.commons.DateUtils;

@Component
public class ShellService {

	private final static Logger logger = LoggerFactory.getLogger(ShellService.class);

	@Value("${caac.mail}")
	private String[] toPeople;

//	@Scheduled(cron = "0/10 * * * * ?")
	private void test0() {
		if (toPeople.length > 0) {
			for (int i = 0; i < toPeople.length; i++) {
				logger.info(toPeople[i]);
			}
		} else {
			logger.info("无数据");
		}
	}

	@Scheduled(cron = "0/10 * * * * ?")
	public void test() throws Exception {
		// 获取
		String filepath = "/home/coder/java-logs/luffy-sync/luffy-sync/caac";
		File file = new File(filepath);// File类型可以是文件也可以是文件夹
		File[] fileList = file.listFiles();// 将该目录下的所有文件放置在一个File类型的数组中
		List<File> logList = new ArrayList<File>();// 新建一个文件集合
		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isFile()) {// 判断是否为文件
				logList.add(fileList[i]);
				logger.info("============>" + fileList[i].getName());
			}
		}

		// 排序
		logList = logList.stream().sorted().collect(Collectors.toList());
		logger.info("======");
		// 巡检开始时间
		String startTimeStr = DateUtils.getDateBeforeOrAfterDays(23, -1, "yyyy-MM-dd");
		logger.info(startTimeStr);
		// 巡检结束时间
		String endTimeStr = DateUtils.getDateBeforeOrAfterDays(10, -1, "yyyy-MM-dd");
		logger.info(endTimeStr);
		// 文件名转换过来的时间
		long fileTime = 0;
		long startTime = new SimpleDateFormat("yyyy-MM-dd").parse(startTimeStr).getTime();
		long endTime = new SimpleDateFormat("yyyy-MM-dd").parse(endTimeStr).getTime();
		// 记录符合时间范围的文件
		List<File> qualifiedFiles = new ArrayList<>();

		// 筛选时间在startTime至endTime的文件
		for (File logfile : logList) {
			fileTime = new SimpleDateFormat("yyyy-MM-dd").parse(logfile.getName().substring(9, 19)).getTime();
			logger.info("fileTime:" + logfile.getName().substring(9, 19) + ",文件时间 :" + fileTime);
			if (fileTime >= startTime && fileTime <= endTime) {
				qualifiedFiles.add(logfile);
			}
		}

		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File("/home/coder/criss/数据上报异常.txt")));
		BufferedWriter write = new BufferedWriter(out);
		// 截取错误文件信息到指定文件
		for (File logfile : qualifiedFiles) {
			String cmdStr = "cat " + logfile.getAbsolutePath() + " | grep -C 5  CAACLOGGER";
			logger.info(cmdStr);
			shellMethod(cmdStr, write, out, startTime, endTime);
		}
		checkCurrentCaacFile(write, startTime, endTime);
		// 关闭文件流
		write.close();
	}

	/**
	 * 执行shell命令截取需要的信息
	 *
	 * @param cmdStr
	 * @param write
	 * @param out
	 * @param startTime
	 * @param endTime
	 */
	private void shellMethod(String cmdStr, BufferedWriter write, OutputStreamWriter out, long startTime,
			long endTime) {
		// 清空文件内容
		clearInfoForFile("/home/coder/criss/数据上报异常.txt");

		Process process = null;
		List<String> processList = new ArrayList<>();
		try {
			String[] cmdArr = { "/bin/sh", "-c", cmdStr };
			process = Runtime.getRuntime().exec(cmdArr);
			int status = process.waitFor();
			if (status != 0) {
				logger.error("Failed to call shell's command ， status:" + status);
			}

			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			while ((line = input.readLine()) != null) {
				processList.add(line);
				write.write(line + "\r\n");
			}

			// 关闭输入流
			input.close();
		} catch (Exception e) {
			logger.error("error", e);
		}

		for (String line : processList) {
			logger.info(line);
		}
	}

	/**
	 * 检查log_caac.log文件是不是在指定时间范围内
	 *
	 * @param write
	 * @param startTime
	 * @param endTime
	 */
	private void checkCurrentCaacFile(BufferedWriter write, long startTime, long endTime) {
		// log_caac文件所在目录
		File file = new File("/home/coder/java-logs/luffy-sync/luffy-sync/log_caac.log");
		BasicFileAttributes bAttributes = null;
		try {
			bAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			// log_caac修改时间 2019-05-08T01:04:52Z
			String changeTime = bAttributes.lastModifiedTime().toString().replace('T', ' ');
			changeTime = changeTime.substring(0, changeTime.length() - 1);
			long changedTime = fixDateFormat(changeTime);

			String startTimeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date(startTime)) + " 00:00:00";
			String endTimeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date(endTime)) + " 23:59:59";
			logger.info("startTime:" + startTimeStr + ", endTime:" + endTimeStr);
			startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTimeStr).getTime();
			endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endTimeStr).getTime();
			if (changedTime >= startTime && changedTime <= endTime) {
				// 将log_caac.log内容写入数据异常上报记录.txt
				String cmdStr = "cat /home/coder/java-logs/luffy-sync/luffy-sync/log_caac.log | grep -C 5 CAACLOGGER";
				String[] cmdArr = { "/bin/sh", "-c", cmdStr };
				Process process = Runtime.getRuntime().exec(cmdArr);
				int status = process.waitFor();
				if (status != 0) {
					logger.error("Failed to call shell's command ， status:" + status);
				}

				BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = "";
				while ((line = input.readLine()) != null) {
					write.write(line + "\r\n");
				}
				input.close();
			}

		} catch (Exception e) {
			logger.error("check and write log_caac.log error", e);
		}

	}

	/**
	 * 加八小时
	 *
	 * @param changTime
	 * @return
	 * @throws Exception
	 */
	private long fixDateFormat(String changTime) throws Exception {
		long time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(changTime).getTime() + 8 * 60 * 60 * 1000;
		return time;
	}

	/**
	 * 清空文件内容
	 *
	 * @param fileName
	 */
	public void clearInfoForFile(String fileName) {
		File file = new File(fileName);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write("");
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			logger.error("clear file text error", e);
		}
	}

}
