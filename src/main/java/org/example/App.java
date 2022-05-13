package org.example;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
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
//            Properties properties = new Properties();
//            File file=new File("src/main/resources/application.properties");
//            InputStream in = new FileInputStream(file);
//            properties.load(in);
//            properties.forEach(
//                    (userName,passWord)->{
//                        try {
//                            sign(userName,passWord);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//            );
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
        driver.navigate().refresh();
        sleep(3000);
        WebElement element = driver.findElement(By.className("btn-sign-in"));
        System.out.println(element);
        element.click();
        sleep(3000);
        driver.quit();
    }

}
