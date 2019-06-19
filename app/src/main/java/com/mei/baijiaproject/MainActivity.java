package com.mei.baijiaproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
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

public class MainActivity extends AppCompatActivity {

    private String testUrl = "http://longnuit.lofter.com/post/1f04bafb_1c5fb57f2?sharefrom=lofter-android-6.2.0&shareto=qq";

    private int line = 0;
    private boolean startParse = false;

    private List<String> results = new ArrayList<>();

    private int picIndex = 0;
    // 添加的图片张数
    private int picTotal = 4;

    private String[] pics = {
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240",
    };

    private Runnable testRunnable = new Runnable() {
        @Override
        public void run() {
            getURLInfo(testUrl);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(testRunnable).start();
            }
        });

    }

    Pattern proInfo
            = Pattern.compile("<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>", Pattern.DOTALL);

    public void getURLInfo(String addressUrl) {
        results = new ArrayList<>();
        picIndex = 0;
        try {
            URL url = new URL(addressUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            Log.e("MainActivity", "result code=" + connection.getResponseCode());
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
                                    results.add(p.trim().substring(0, p.indexOf("   ")));
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



            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void copyTextToClipboard(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("playerId", text);
                clipboardManager.setPrimaryClip(clipData);
            }
        });
    }

}
