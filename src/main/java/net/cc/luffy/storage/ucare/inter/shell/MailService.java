package net.cc.luffy.storage.ucare.inter.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.cc.commons.DateUtils;

/**
 * @author wangqiubao
 * @date 2019-05-20 11:28
 * @description 定时发送民航局共享数据错误给民航局和王总
 */
@Service
@Slf4j
public class MailService {

    @Autowired
    JavaMailSender jms;

    /**
     * 邮件出处
     */
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 邮件发往处
     */
    @Value("${caac.mailArr}")
    private String[] toPeople;

    /**
     * 历史日志目录
     */
    @Value("${caac.logDir}")
    private String logDir;

    /**
     * 给民航局发送的日志文件
     */
    @Value("${caac.uploadFile}")
    private String uploadFile;

    /**
     * log_caac.log日志
     */
    @Value("${caac.logFile}")
    private String logFile;

    /**
     * 每周一的九点发一份邮件
     */
    public void sendMailToCaac() {

        String[] fileArray = {uploadFile};

        MimeMessage message = jms.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(toPeople);
            helper.setSubject("无人机云数据交换平台巡检周报(优凯)"); // 主题
            helper.setText(mailText()); // 邮件正文
            createExceptionFile(); // 生成邮件附件
            // 验证文件数据是否为空
            if (null != fileArray) {
                FileSystemResource file = null;
                for (int i = 0; i < fileArray.length; i++) {
                    // 添加附件
                    file = new FileSystemResource(fileArray[i]);
                    helper.addAttachment("数据上报异常记录.txt", file);
                }
            }
            jms.send(message);
            log.info("带附件的邮件发送成功");
        } catch (Exception e) {
            log.error("发送带附件的邮件失败", e);
        }
    }

    /**
     * 设置邮件正文
     *
     * @return
     */
    public String mailText() {
        StringBuilder sBuilder = new StringBuilder();
        // 巡检开始时间
        String startTime = DateUtils.getDateBeforeOrAfterDays(7, -1, "yyyy年MM月dd日");
        // 巡检结束时间
        String endTime = DateUtils.getDateBeforeOrAfterDays(1, -1, "yyyy年MM月dd日");
        // 上报时间
        String uploadTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        sBuilder.append("		无人机云数据交换平台巡检周报 \n");
        sBuilder.append("			发布单位：优凯 \n");
        sBuilder.append("巡检时间周期：（").append(startTime).append("至").append(endTime + ")\n");
        sBuilder.append("报告日期：").append(uploadTime + "\n");
        sBuilder.append("报告内容需包括以下内容：时间，巡检次数，云系统实时性 \n");
        sBuilder.append("按照要求，优凯巡检工作人员在").append(startTime).append("至").append(endTime + "\n")
                .append("对系统进行了共计28次巡检。巡检记录如下： \n");
        sBuilder.append("系统运行情况：\n");
        sBuilder.append("系统运行正常。\n");
        sBuilder.append("数据交换情况：\n");
        sBuilder.append("数据交换正常，电子围栏更新正常，数据上报异常见“数据上报异常记录”。");
        return sBuilder.toString();
    }

    /**
     * 设置附件内容，即往数据上报异常.txt写入过滤的信息
     *
     * @throws Exception
     */
    public void createExceptionFile() throws Exception{
        // 获取指定目录下的文件
        String filepath = logDir;
        File file = new File(filepath);// File类型可以是文件也可以是文件夹
        File[] fileList = file.listFiles();// 将该目录下的所有文件放置在一个File类型的数组中
        List<File> logList = new ArrayList<File>();// 新建一个文件集合

        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {// 判断是否为文件
                logList.add(fileList[i]);
            }
        }
        // 文件排序
        logList = logList.stream().sorted().collect(Collectors.toList());

        // 巡检开始时间
        String startTimeStr = DateUtils.getDateBeforeOrAfterDays(7, -1, "yyyy-MM-dd");
        // 巡检结束时间
        String endTimeStr = DateUtils.getDateBeforeOrAfterDays(1, -1, "yyyy-MM-dd");
        // 文件名转换过来的时间
        long fileTime = 0;
        long startTime = new SimpleDateFormat("yyyy-MM-dd").parse(startTimeStr).getTime();
        long endTime = new SimpleDateFormat("yyyy-MM-dd").parse(endTimeStr).getTime();
        // 记录符合时间范围的文件
        List<File> qualifiedFiles = new ArrayList<>();
        // 筛选时间在startTime至endTime的文件
        try{
            for (File logfile : logList) {
                fileTime = new SimpleDateFormat("yyyy-MM-dd").parse(logfile.getName().substring(9, 19)).getTime();
                if (fileTime >= startTime && fileTime <= endTime) {
                    qualifiedFiles.add(logfile);
                }
            }
        }catch(Exception e){
            log.error("查找符合文件异常", e);
        }

        try(OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File(uploadFile)));
            BufferedWriter write = new BufferedWriter(out)){
            // 截取错误文件信息到指定文件
            for (File logfile : qualifiedFiles) {
                String cmdStr = "cat " + logfile.getAbsolutePath() + " | grep -C 5  CAACLOGGER";
                shellMethod(cmdStr, write, startTime, endTime);
            }

            // 检查log_caac文件
            checkCurrentCaacFile(write, startTime, endTime);
        }catch(Exception e){
            // 关闭文件流
            log.error("读写文件异常！", e);
        }
    }

    /**
     * 执行shell命令截取需要的信息
     *
     * @param cmdStr
     * @param write
     * @param startTime
     * @param endTime
     */
    private void shellMethod(String cmdStr, BufferedWriter write, long startTime, long endTime) {
        Process process = null;
        try {
            String[] cmdArr = {"/bin/sh", "-c", cmdStr};
            process = Runtime.getRuntime().exec(cmdArr);
            readStreamInfo(write, process.getInputStream());
            int status = process.waitFor();
            if (status != 0) {
                throw new RuntimeException("Failed to call shell's command ， status:" + status);
            }

        } catch (Exception e) {
            log.error("Failed to call shell's command", e);
        }
    }

    public void readStreamInfo(BufferedWriter write, InputStream... inputStreams){
        ExecutorService executorService = Executors.newFixedThreadPool(inputStreams.length);
        for (InputStream in : inputStreams) {
            executorService.execute(new MailThread (write, in));
        }
        executorService.shutdown();
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
        File file = new File(logFile);
        BasicFileAttributes bAttributes = null;
        try {
            bAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            // log_caac修改时间
            String changeTime = bAttributes.lastModifiedTime().toString().replace('T', ' ');
            changeTime = changeTime.substring(0, changeTime.length() - 1);
            long changedTime = fixDateFormat(changeTime);

            String startTimeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date(startTime)) + " 00:00:00";
            String endTimeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date(endTime)) + " 23:59:59";
            startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTimeStr).getTime();
            endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endTimeStr).getTime();
            if (changedTime >= startTime && changedTime <= endTime) {
                // 将log_caac.log内容写入数据异常上报记录.txt
                String cmdStr = "cat " + logFile + " | grep -C 5 CAACLOGGER";
                String[] cmdArr = {"/bin/sh", "-c", cmdStr};
                Process process = Runtime.getRuntime().exec(cmdArr);
                int status = process.waitFor();
                if (status != 0) {
                    throw new RuntimeException("Failed to call shell's command ， status:" + status);
                }

                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = "";
                while ((line = input.readLine()) != null) {
                    write.write(line + "\r\n");
                }
                input.close();
            }
        } catch (Exception e) {
            log.error("check log_caac.log error or exec shell fail", e);
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
            log.error("clear file text error", e);
        }
    }
}

/**
 *
 * @author wangqiubao
 *
 * @date 2019-08-05 16:23
 *
 * @description
 */
class MailThread implements Runnable {
    private InputStream in;
    private BufferedWriter write;
    public MailThread(BufferedWriter write, InputStream in){
    	this.write = write;
        this.in = in;
    }

    @Override
    public void run() {
    	synchronized(write) {
    		try{
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line = null;
                while((line = br.readLine())!=null){
                	write.write(line + "\r\n");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    	}

    }
}
