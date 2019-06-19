package com.mei.baijiaproject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/19
 */
public class TestMD {

    private static String testUrl = "http://longnuit.lofter.com/post/1f04bafb_1c5fb57f2?sharefrom=lofter-android-6.2.0&shareto=qq";

    private static int line = 0;
    private static boolean startParse = false;

    private static List<String> results = new ArrayList<>();

    private static int picIndex = 0;
    // 添加的图片张数
    private static int picTotal = 4;

    private static String[] pics = {
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
    };

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getURLInfo(testUrl);
            }
        }).start();
    }

    static Pattern proInfo
            = Pattern.compile("<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>", Pattern.DOTALL);

    public static void getURLInfo(String addressUrl) {
        results = new ArrayList<>();
        picIndex = 0;
        try {
            URL url = new URL(addressUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                connection.connect();
                InputStream is = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    //这里是对链接进行处理
                    line = line.replaceAll("</?a[^>]*>", "");
                    //这里是对样式进行处理
                    line = line.replaceAll("<(\\w+)[^>]*>", "<$1>");
                    sb.append(line);
                }
                is.close();
                br.close();
                connection.disconnect();

                String[] info = sb.toString().trim().split("</li>");

                for (String s : info) {
                    Matcher m = proInfo.matcher(s);
                    if (m.find()) {
                        String[] result = m.group().trim().split("</p>");
                        for (String p : result) {
                            p = p.trim();
                            if (p.startsWith("<p><strong>")) {
                                if (startParse) continue;
                                startParse = true;
                                p = p.replaceAll("<p><strong><span>", "");
                                p = p.replaceAll("<br></span></strong>", "");
                                results.add(p);
                                continue;
                            }

                            if (startParse) {

                                // 添加标签
                                if (p.startsWith("</div>")) {
                                    p = p.replaceAll("</div>", "");
                                    //这里是对样式进行处理
                                    p = p.replaceAll("<div>", "");
                                    // .substring(0, p.indexOf("   "))
                                    results.add(p.trim());
                                    break;
                                }

                                p = p.replaceAll("<p>", "");
                                p = p.replaceAll("<br>", "");
                                if (!p.trim().equals("") && !p.trim().equals("&nbsp;")) {
                                    results.add(p);
                                }
                            }

                        }
                    }
                }

                int size = results.size();
                int count = size / 5;
                for (int i = 0; i < picTotal; i++) {
                    results.add(count + i * count + new Random().nextInt(3), "![](" + pics[i] + ")");
                }

                File file = new File("D:\\01.md");
                if (!file.exists()) {
                    file.createNewFile();
                }

                StringBuilder ssb = new StringBuilder();
                int partIndex = 0;
                for (String s : results) {
                    partIndex++;
                    ssb.append(s + (partIndex % 4 == 0 ? "\n\n" : ""));
                }

                if (file.isFile()) {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(ssb.toString());
                    fileWriter.flush();
                    fileWriter.close();
                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
