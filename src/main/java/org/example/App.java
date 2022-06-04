package org.example;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static java.lang.Thread.sleep;

public class App 
{
    public static void main(String[] args)
    {
        try {
            InputStream resourceAsStream = App.class.getResourceAsStream("/application.properties");
            BufferedReader br=new BufferedReader(new InputStreamReader(resourceAsStream));
            String s="";
            while((s=br.readLine())!=null) {
                sign(s.split("=")[0],s.split("=")[1]);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sign(String userName,String passWord) throws InterruptedException {
        WebDriverManager.chromedriver().setup();
        ChromeDriver driver = new ChromeDriver();
        driver.get("https://sg.qq.com/cp/a20200228signin/index.html");
        sleep(3000);
        WebDriver loginFrame = driver.switchTo().frame(driver.findElement(By.id("loginFrame")));
        loginFrame.findElement(By.id("u")).sendKeys(userName);
        loginFrame.findElement(By.id("p")).sendKeys(passWord);
        loginFrame.findElement(By.id("go")).click();
        sleep(3000);
        //出现验证码
        if (isElementPresent(driver,By.id("tcaptcha_iframe"))){
            check(driver);
        }
        driver.navigate().refresh();
        sleep(3000);
        WebElement element = driver.findElement(By.className("btn-sign-in"));
        System.out.println(element);
        element.click();
        sleep(3000);
        driver.quit();
    }

    public static void check(ChromeDriver driver) throws InterruptedException {
        boolean flag = false;
        while (!flag){
            //切换到验证码所在的iframe
            WebElement tcaptcha_iframe = driver.findElement(By.id("tcaptcha_iframe"));
            // WebDriver tcaptcha_iframe = driver.switchTo().frame(driver.findElement(By.id("tcaptcha_iframe")));
            WebDriver frame = driver.switchTo().frame(tcaptcha_iframe);
            //定位滑块图片
            WebElement slideBlock = frame.findElement(By.id("slideBlock"));
            //定位验证码背景图
            WebElement slideBg = frame.findElement(By.id("slideBg"));
            //获取图片Url链接
            String slideBlockUrl = slideBlock.getAttribute("src");

            String slideBgUrl = slideBg.getAttribute("src");
            //下载对应图片
            downloadImg(slideBlockUrl,"slideBlock.png");
            downloadImg(slideBgUrl,"slideBg.png");
            double  slideDistance=getSlideDistance(System.getProperty("user.dir")+"slideBlock.png",System.getProperty("user.dir")+"slideBg.png");


            Actions actions = new Actions(frame);

            WebElement tcaptcha_drag_thumb = frame.findElement(By.id("tcaptcha_drag_thumb"));
            //获取style属性值，其中设置了滑块初始偏离值  style=left: 23px;
            // 需要注意的是网页前端图片和本地图片比例是不同的，需要进行换算
            slideDistance = slideDistance / 2 - 23;
            actions.clickAndHold(tcaptcha_drag_thumb);
            //根据滑动距离生成滑动轨迹，约定规则：开始慢->中间快->最后慢
            List<Integer>moveTrack= getMoveTrack((int)slideDistance);
            for (Integer index : moveTrack) {
                actions.moveByOffset(index, 0).perform();
            }
            actions.release().perform();
            //判断是否通过
            sleep(4000);
            driver.switchTo().parentFrame();
            if (!isElementPresent(driver,By.id("tcaptcha_iframe"))){
                flag = true;
            }
        }


    }

    private static double getSlideDistance(String slideBlock, String slideBg) {
        // 加载OpenCV本地库
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //对滑块进行处理
        Mat slideBlockMat=Imgcodecs.imread(slideBlock);
        //1、灰度化图片
        Imgproc.cvtColor(slideBlockMat,slideBlockMat,Imgproc.COLOR_BGR2GRAY);
        //2、去除周围黑边
        for (int row = 0; row < slideBlockMat.height(); row++) {
            for (int col = 0; col < slideBlockMat.width(); col++) {
                if (slideBlockMat.get(row, col)[0] == 0) {
                    slideBlockMat.put(row, col, 96);
                }
            }
        }
        //3、inRange二值化转黑白图
        Core.inRange(slideBlockMat, Scalar.all(96), Scalar.all(96), slideBlockMat);
        //对滑动背景图进行处理
        Mat slideBgMat = Imgcodecs.imread(slideBg);
        //1、灰度化图片
        Imgproc.cvtColor(slideBgMat,slideBgMat,Imgproc.COLOR_BGR2GRAY);
        //2、二值化
        Imgproc.threshold(slideBgMat,slideBgMat,127,255, Imgproc.THRESH_BINARY);
        Mat g_result = new Mat();
        /*
         * matchTemplate：在模板和输入图像之间寻找匹配,获得匹配结果图像
         * result：保存匹配的结果矩阵
         * TM_CCOEFF_NORMED标准相关匹配算法
         */
        Imgproc.matchTemplate(slideBgMat,slideBlockMat,g_result, Imgproc.TM_CCOEFF_NORMED);
        /* minMaxLoc：在给定的结果矩阵中寻找最大和最小值，并给出它们的位置
         * maxLoc最大值
         */
        Point matchLocation= Core.minMaxLoc(g_result).maxLoc;
        //返回匹配点的横向距离
        return matchLocation.x;
    }

    private static void downloadImg(String slideBlockUrl, String filename) {
        System.out.println("开始下载");
        filename = System.getProperty("user.dir")+filename;
        try{
            URL url = new URL(slideBlockUrl);
            URLConnection con = url.openConnection();
            InputStream is = con.getInputStream();
            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的⽂件流
            File file = new File(filename);
            FileOutputStream os = new FileOutputStream(file, false);
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
            System.out.println("下载完成");
            os.close();
            is.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Integer> getMoveTrack(int distance) {
        List<Integer> track = new ArrayList<>();// 移动轨迹
        Random random = new Random();
        int current = 0;// 已经移动的距离
        int mid = (int) distance * 4 / 5;// 减速阈值
        int a = 0;
        int move = 0;// 每次循环移动的距离
        while (true) {
            a = random.nextInt(10);
            if (current <= mid) {
                move += a;// 不断加速
            } else {
                move -= a;
            }
            if ((current + move) < distance) {
                track.add(move);
            } else {
                track.add(distance - current);
                break;
            }
            current += move;
        }
        return track;
    }

    public static boolean isElementPresent(WebDriver driver, By by){
        try{
            driver.findElement(by);
            return true;
        }catch(Exception e){
            return false;
        }
    }

}
