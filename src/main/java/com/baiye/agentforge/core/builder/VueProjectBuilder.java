package com.baiye.agentforge.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.io.File;

/**
 * ClassName: VueProjectBuilder
 * Package: com.baiye.agentforge.core.builder
 * Description: Vue 项目构建器
 *
 * @Author 白夜
 * @Create 2026/5/18 11:03
 * @Version 1.0
 */
@Slf4j
@Component
public class VueProjectBuilder {


    /**
     * 异步构建项目（不阻塞主流程）
     * 返回值：void – 方法立即返回，不等待构建结束，也不把构建结果（boolean）返回给调用方。
     * <p>
     * Thread.ofVirtual()
     * Java 21 引入的虚拟线程（Project Loom 正式特性）。
     * 虚拟线程是轻量级用户态线程，由 JVM 在少数操作系统线程上调度，创建成本极低（不像传统平台线程需要 1:1 占用 OS 线程）。
     * 特别适合高并发、大量 IO 阻塞的任务（例如这里的 npm install 会大量等待磁盘 IO 和网络 IO），能避免耗尽线程池或 OS 线程资源。
     * .name("vue-builder-" + System.currentTimeMillis())
     * 为虚拟线程设置名称，便于调试和日志跟踪。
     * 名称包含当前毫秒时间戳，可以区分不同时间发起的构建任务。
     * 潜在问题：如果同一毫秒内多次调用，会创建多个同名线程（但不影响功能）。
     * .start(Runnable)
     * 启动虚拟线程，执行传入的 Runnable。
     * Runnable 内部会调用之前定义的 buildProject(projectPath)，并捕获所有异常。
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }


    /**
     * 执行一条命令行指令，在给定的工作目录中运行，并设定超时时间。最终根据命令的退出码判断执行是否成功。
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            //workingDir.getAbsolutePath() 将 File 对象的路径转为绝对路径字符串。
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            //让操作系统创建一个新的进程来执行 command.split("\\s+") 所表示的命令和参数，并让该进程的当前工作目录指向 workingDir。
            Process process = RuntimeUtil.exec(
                    null,//环境变量数组，null 表示继承父进程的环境变量。
                    workingDir,//指定子进程的工作目录。
                    //将命令字符串按空白字符（空格、制表符等）拆分成数组。例如 "ls -l /home" 变成 ["ls", "-l", "/home"]。
                    command.split("\\s+")
            );
            // process.waitFor(long timeout, TimeUnit unit),阻塞当前线程直到进程结束或超时。
            //返回值 finished：true：进程在指定时间内正常结束。false：等待超时，进程仍在运行。
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();//强制终止子进程
                return false;//表示本次执行结果为失败。
            }
            int exitCode = process.exitValue();//进程正常结束后，获取退出码：
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * 判断当前操作系统是否是 Windows 系统
     * System.getProperty("os.name")
     * Java 标准库方法，用于读取 JVM 的系统属性。"os.name" 这个键对应的值就是操作系统的名称。
     * 例如，在 Windows 系统上，这个值会是 "Windows 10" 或 "Windows 11"。
     * .toLowerCase()
     * 将得到的系统名称字符串转换为全小写字母。
     * .contains("windows")
     * String 的 contains 方法，判断字符串中是否包含指定的子串。
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    //根据操作系统构造命令的方法
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 执行 npm install 命令
     * String.format 创建格式化的字符串。
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }


    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);//创建一个代表项目目录的 File 对象，
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");//在项目目录下构造一个指向 package.json 的 File 对象。
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");//在项目目录下构造一个指向 dist 目录的 File 对象。
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }


}
