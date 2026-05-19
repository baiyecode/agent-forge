package com.baiye.agentforge.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.UUID;
import java.io.File;
import java.time.Duration;

/**
 * ClassName: WebScreenshotUtils
 * Package: com.baiye.agentforge.utils
 * Description: Web 截图工具类
 *
 * @Author 白夜
 * @Create 2026/5/19 11:22
 * @Version 1.0
 */
@Slf4j
public class WebScreenshotUtils {


    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    // 使用 ThreadLocal 为每个线程维护独立的 WebDriver
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    /**
     * 获取当前线程的 WebDriver（懒加载，自动创建）
     */
    private static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            driver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            driverThreadLocal.set(driver);
            log.debug("为线程 {} 创建新的 WebDriver", Thread.currentThread().getName());
        }
        return driver;
    }

    /**
     * 关闭并移除当前线程的 WebDriver
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("关闭 WebDriver 失败", e);
            } finally {
                driverThreadLocal.remove();
                log.debug("已关闭并移除线程 {} 的 WebDriver", Thread.currentThread().getName());
            }
        }
    }

    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 保存图片到文件
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            //将字节数组写入指定路径的文件。如果文件所在目录不存在，该方法通常会自动创建父目录；如果文件已存在，默认会覆盖。
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        // 压缩图片质量（0.1 = 10% 质量）,数值越大质量越高、文件越大，数值越小质量越低、文件越小。
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    //通过 FileUtil.file(originalImagePath) 将字符串路径包装为 File 对象。
                    FileUtil.file(originalImagePath),//源文件对象（File）
                    FileUtil.file(compressedImagePath),// 目标文件对象（File）
                    COMPRESSION_QUALITY //压缩质量，浮点数 0.3。
            );
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    /**
     * 等待页面加载完成
     *
     * 为什么不用 Selenium 自带的页面加载策略替代？
     * 之前的配置中，driver.manage().timeouts().pageLoadTimeout(30) 已经让 driver.get() 等待 complete。
     * 但实际经验发现，有些页面在 get() 返回后仍有异步 XHR 请求或前端框架的渲染任务在执行，
     * 此时 readyState 可能已经是 complete，但 DOM 上的元素尚未更新完毕。下面通过额外 sleep 来缓解这个问题。
     * 如果缺少 waitForPageLoad，就可能截到“白屏”、“部分加载”或“骨架屏”状态的页面，导致截图不可用。
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象,每隔一段固定时间（默认 500 毫秒）轮询条件，直到条件为真或超时。
            //这里构造时指定了最长等待时间为 10 秒。如果超过 10 秒条件仍不满足，会抛出 TimeoutException。
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            //每次轮询时，Lambda 会：
            //将传入的 webDriver 强制转换为 JavascriptExecutor（ChromeDriver 已经实现了该接口），
            // 然后调用 executeScript("return document.readyState") 在当前页面上下文中执行 JavaScript 代码。
            //获取返回值，与字符串 "complete" 做比较。
            //只有当 document.readyState 变为 "complete" 时，until 方法才会返回，代码才会继续往下执行。
            //这种等待方式更加可靠，因为它直接读取浏览器的内部状态，不受网络波动或个别资源阻塞的影响
            // Selenium 的默认页面加载策略在某些条件下可能已经返回但页面中还有异步资源未完成，但 readyState 会反映最终加载完成。
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            //没有抛出异常：这与之前的 saveImage 和 compressImage 不同，它仅仅记录错误日志，然后让代码继续执行。
            //这样做的设计意图是：即使页面加载等待超时或出现其他脚本错误，也不影响后续截图——哪怕当前页面尚未完全渲染完毕，
            // 也可以“将错就错”截取当前状态。因为对于截图工具来说，没有截图比截图不完美更糟糕。
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }


    /**
     * 生成网页截图
     * 创建目录 → 访问页面 → 显式等待 → 截图 → 保存原始 → 压缩 → 删除原始 → 返回压缩路径。
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }
        try {
            // 创建临时目录,8 位 UUID 前缀（例如 a1b2c3d4），确保每次截图都有独立的子目录，避免多线程或多任务时的文件名冲突。
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);//会递归创建多级目录，如果父目录 tmp/screenshots 不存在也会一并创建。
            // 图片后缀
            final String IMAGE_SUFFIX = ".png";
            // 原始截图文件路径,随机数字进一步避免了同一目录内文件名冲突
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 访问网页
            WebDriver driver = getDriver();          // 获取本线程的 driver
            driver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(driver);
            // 截图.截取当前视口内的完整页面（即浏览器窗口看到的部分，不是整个长页面，
            // 除非后续有滚动拼接处理，但这里只截取当前视口 1600×900）。返回 PNG 格式的字节数组。
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);
            // 压缩图片
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            //5 位随机数 + _compressed.jpg。注意后缀由 .png 变成了 .jpg，
            //因为 Hutool 的 ImgUtil.compress 会根据目标文件扩展名来编码，
            //这里指定为 .jpg 就能实现有损压缩（质量系数 0.3），大幅减小文件体积。
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩图片保存成功: {}", compressedImagePath);
            // 删除原始图片，只保留压缩图片,这样可以节省磁盘空间，因为 PNG 原图体积可能数倍于 JPG。
            FileUtil.del(imageSavePath);
            return compressedImagePath;//返回压缩文件的完整路径：调用者拿到这个路径就可以直接使用或上传到云存储等。
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            // 如果出现严重异常，销毁当前 driver，下次重新创建
            quitDriver();
            return null;
        }
    }




}

