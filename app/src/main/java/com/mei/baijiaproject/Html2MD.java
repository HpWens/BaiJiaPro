package com.mei.baijiaproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
 * @since 2019/7/1
 */
public class Html2MD {


    public static String[] parseHtmlArray = {
            "http://knowbox.lofter.com/post/1d0f388e_b0e831d?sharefrom=lofter-android-6.2.6&shareto=qq"
    };

    private static List<String> picList = new ArrayList<>();

    private static List<String> MDResultList = new ArrayList<>();

    private static Pattern proInfo
            = Pattern.compile("<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>", Pattern.DOTALL);

    private static int MAX_PART_NUM = 20;

    private static int MAX_PIC_NUM = 5;

    public static void main(String[] args) {
        // 请求网络
        new Thread(new Runnable() {
            @Override
            public void run() {
                picList.clear();
                for (String pic : pics) {
                    picList.add(pic);
                }
                requestHttp();
            }
        }).start();
    }

    private static void requestHttp() {
        for (String url : parseHtmlArray) {
            requestHtml2MD(url);
        }
    }

    private static void requestHtml2MD(String url) {
        try {
            URL urlHtml = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlHtml.openConnection();
            if (conn.getResponseCode() == 200) {
                conn.connect();
                InputStream is = conn.getInputStream();
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
                conn.disconnect();

                String[] liArray = sb.toString().trim().split("</li>");

                boolean isParseHtml = false;

                for (String li : liArray) {
                    Matcher m = proInfo.matcher(li = li.replaceAll(" ", ""));
                    if (m.find()) {
                        String input = m.group().trim();
                        if (input.length() < 500) {
                            input = li.substring(li.indexOf("<h2>"));
                        }
                        String[] result = input.split("</p>");
                        for (String p : result) {
                            // 去除空格符号
                            p = p.replaceAll(" ", "");

                            boolean filter = p.startsWith("<p><strong>") || p.startsWith("<br>")
                                    || p.startsWith("<p><br>") || p.contains("<h2>");

                            System.out.println("**" + p + "***" + filter);

                            if (!isParseHtml && filter) {
                                isParseHtml = true;
                                p = p.replaceAll("<p><strong><span>", "");
                                p = p.replaceAll("<br></span></strong>", "");
                                p = p.replaceAll("<p>", "");
                                p = p.replaceAll("<br>", "");
                                MDResultList.add(p);
                            }

                            if (!isParseHtml) continue;


                            if (isParseHtml) {

                                // 结束标签
                                if (p.startsWith("</div>")) {
                                    p = p.replaceAll("</div>", "");
                                    p = p.replaceAll("<div>", "");
                                    MDResultList.add(p.trim());
                                    break;
                                }

                                // 添加段落
                                p = p.replaceAll("<p>", "");
                                p = p.replaceAll("<br>", "");
                                if (!p.trim().equals("") && !p.trim().equals("&nbsp;")) {
                                    MDResultList.add(p);
                                }

                            }

                        }
                    }

                    // 解析停止
                    break;
                }

                if (MDResultList.isEmpty()) {
                    return;
                }

                parseData();
            }
        } catch (Exception e) {

        }
    }

    private static void parseData() {
        // 第一步 判定生成多少个MD文件
        StringBuilder ssb = new StringBuilder();

        int fileNum = 1;
        fileNum = MDResultList.size() / MAX_PART_NUM;
        if (fileNum == 0) fileNum = 1;

        List<String> everyArticle = new ArrayList<>();
        int parts = MDResultList.size() / fileNum;
        for (int i = 0; i < fileNum; i++) {
            ssb = new StringBuilder();
            List<String> data = new ArrayList<>();
            data.addAll(MDResultList);

            everyArticle = new ArrayList<>();
            everyArticle.addAll(data.subList(i * parts, (i == (fileNum - 1)) ? data.size() : (i + 1) * parts));

            System.out.println("" + everyArticle.size() + "****" + i);

            List<String> picList = getPicArray();

            // 添加开篇段落
            ssb.append("**想看有趣的《魔道祖师》番外吗？这里有可爱的皮皮羡；撩人的汪叽；正直的舅舅；还有虐心的薛晓。可爱的道友们别忘记点击右上方的关注哟~~**");
            ssb.append("\n\n");

            // 添加图片
            int size = everyArticle.size() / picList.size();

            if (everyArticle.size() > picList.size()) {
                for (int j = 0; j < picList.size(); j++) {
                    everyArticle.add((j + 1) * size,
                            "\n\n ### " + getTitle(j) + "\n"
                                    + "![](" + picList.get(j) + ")" + "\n");
                }
            } else {
                everyArticle.add(everyArticle.size() / 2, picList.get(0));
            }

            // 最后将两段合成一段 出去图片与标题
            int part = 1;
            for (String s : everyArticle) {
                if (!s.startsWith("#") && !s.startsWith("!")) {
                    part++;
                }
                ssb.append(s + (part % 2 == 0 ? "\n\n" : fileNum == 1 ? "\n\n" : ""));
            }

            // 添加结尾段落
            ssb.append("**想看更多更精彩的《魔道祖师》番外，别忘记点击右上角关注：小爱说动漫~~**");
            ssb.append("\n\n");

            try {
                File file = new File("D:\\0" + i + ".md");
                if (!file.exists()) {
                    file.createNewFile();
                }

                if (file.isFile()) {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(ssb.toString());
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (Exception e) {

            }
        }

    }

    private static String getTitle(int num) {
        return "番外0" + (num + 1);
    }

    private static List<String> getPicArray() {
        List<String> pics = new ArrayList<>();
        for (int i = 0; i < MAX_PIC_NUM; i++) {
            int index = new Random().nextInt(picList.size());
            pics.add(picList.get(index));
            picList.remove(index);
        }
        return pics;
    }

    private static String[] pics = {
            "https://upload-images.jianshu.io/upload_images/2258857-5b9cebccc285042f.jpg",
            "http://imglf4.nosdn0.126.net/img/N2Y3NS8zQXlmREFScTJGWmgxQ3VjNGtqKzRrQTY3ak9zV1NEaFlkMVFNZGtSQXprVXZhN1JBPT0.jpg",
            "http://imglf4.nosdn0.126.net/img/N2Y3NS8zQXlmREFScTJGWmgxQ3VjNEtweVp3SExHYlN0VWlUcEtFTktoQWhPQ2hqSHBic3JnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-f50de0ffebdc2ebd.jpg",
            "http://imglf6.nosdn0.126.net/img/WklxcW02eXd4SVI0dlkwR3Q3NHlCd2FSc2s2bU1CTExLMWhHRWVUc2FuR1JvYXc5R0Y3eFp3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-13be647666dfd7f6.jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-082a0f5e022d4c5e.jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-15ebdbc3032796c2.jpg",
            "http://imglf4.nosdn0.126.net/img/dXl2M2Fjc2FiZ1k2SVJuZ2R4MFpyOUlReG9CM3ZlclVabldncDFXZ3BpU1FySCt4VWtLY1d3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-5c7abbbe925a4912.jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-025c5909e4dfda17.jpg",
            "http://imglf4.nosdn0.126.net/img/a21CYUFtRnZxZ1Nkb2dVN29KZW40QVFnbExhSlFERDJHQldFclpzZzYxbWZJNXVMRzVZVzRBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-be25f4ae5497d3fb.jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-543e208fefa149ee.jpg",
            "https://upload-images.jianshu.io/upload_images/2258857-55eb180c402400f3.jpg",
            "http://imglf4.nosdn0.126.net/img/TEQ0dWkxaXNRdlVnNHlHNjN5djR2SHNsSHJvemVSZm9OTjVpWkNsNEQ0WUVUeTNCdWtHclZRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExyamdma0h2U2Z1UEQ2bG1LeVJpNGFHdnlZWlRlbGxWcEZnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExydHVMTEx0cXVBVGlUYnk3Mlp4TVloS1pyTEZkKzJjTldBPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf5.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExyZ3VaTzNEM2RBRXBvYkdRNDRHMWZZa2orV2cyTWY2cXZnPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf3.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExydG1GWTFRek9TT2gzL2hIWGZnNkkyVUNYbElUTkRYa0RnPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf3.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExycjJrUDZKb3EvK0JDZncrcUFkTWdkNy9mZ2VLTlBPVkdRPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf6.nosdn0.126.net/img/Nzd1V3RpRVM5OWFUT09mSTlDZExya2pobzN2MFhlK04ySXhQb3cvaTUrTEdxUFJvdzhDeURnPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf6.nosdn0.126.net/img/Vk16cW5ja1JReGxNTFcyQ0J6TlRVVmU0b1BualhOSE9JZHNNWXB0RXhqbU0yOExZZXl0aElnPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf6.nosdn0.126.net/img/YUVWUVM2TXNMaVA1T1NoVG9GQ2Q3a3lqR2M2SkExS2dmcDQvZ2FpTXYyeWZ4emVxRGxRLzZ3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/YUVWUVM2TXNMaVA1T1NoVG9GQ2Q3bzBsdkJ3VWVKU2E1bFhocTlmbG9TWmhjMnBRUVdoTGN3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/TEQ0dWkxaXNRdlhRRUIyWEJNcHFBajVXWVAvUkl6VERoNDRDQm1aQmJBR3RWb3UvU2N3Rml3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/WnBHWFRDTG90VTc3b0VPUUhDN2dlNnpKWTc5dXR6eE1WOGFNczE4QVYxNW1qVjZOTXRXdTZBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/WnBHWFRDTG90VTdjUEhEeDJFb0ZuNExia2dTYXJtc1ErdXVnWS9vUGE0NkNrRWtsYXFDdlRnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/WnBHWFRDTG90VTU2VVFadnJQb3RvYXFaZ3lJem1PaER4dlJFNUQzMjJqb1YzS2pTakpCSHlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/WnBHWFRDTG90VTU2VVFadnJQb3RvYVlvRW1GeVBnNjduNDRodTNKQ2M1eUF2YTdlRkpzSFFBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/WnBHWFRDTG90VTU5cUMzbG9WMk81amhndjdYWFA4VHg1QXJvNTVkUkNOUmVRS2tGZzZNRFF3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/WnBHWFRDTG90VTU5cUMzbG9WMk81bzNCRHRTMGM2ZTZSZW43ekxSZHFad2hTSDV5UXRrSkN3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/WnBHWFRDTG90VTR1YzdVU0pXREZLU1JhaEUvWkpjdFZHamhWaUYwT1V5TDFuTkxXdHhNamNBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/WnBHWFRDTG90VTZZRXFLajNYSHpxY3dvYUlabWpJczBwVnZXdnF2c2dvOHl0a1grdTgyNnJ3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/clBMd0JTVUJ1L1dqTDBQYkREbjFrUnNUUkQ2S3lXTjVUUlRXWGZJdGU4bWd0VjFjTGNnUG1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/clBMd0JTVUJ1L1hRTG1HK0FoRGkxTmZKTEh5RjFlT0pibCswWHFKcXcvUTVyN0RjUnFxMVlBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf2.nosdn.127.net/img/clBMd0JTVUJ1L1VCdEhrdTVnb2ViWExvYXV1VlNDcmlTWGpuM2pVMVQxbGExamFHMFlOVDZnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/clBMd0JTVUJ1L1dqRWdqYi9IL2JhWk83dmtWdlUyaUlqRmQwbGRNRjR4RVY2eTRZSXBnTW1BPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/clBMd0JTVUJ1L1hleHVZT3dDelV1RDhWb0NMTzlGT21kTEdEVjgrVWw5NVhLN2h2NjdvV0pnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/clBMd0JTVUJ1L1ZnLzM2KzBFSHhIRUtlem5GbHNGTGdTUXRVOGh1SjRrUFVCVTcwZk1XcUJRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/clBMd0JTVUJ1L1duZVdVVmFUSUpWU2dmNnA5azV1dmlnSzMvcHlKMXFDSHFSVzFrbXdkbkNBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/clBMd0JTVUJ1L1dlSENSOGlwbXRJZ0dwQ0ZwYS92MkczY1pBQ0FTSnpWVk04VlpNY2MwZi9BPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/clBMd0JTVUJ1L1dKemc3bDI3SGpMYjhrcm9vVlhjT0NjSllLYXM5OHkzaEpheWVQRDJERVl3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/clBMd0JTVUJ1L1VKRVoyUWl6ZksvWjB1WGJsTEdLeEMvY3pXQzgxZWdrQnR4SlFqUmkyZGNnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/clBMd0JTVUJ1L1diVUl6ZDFJRUVqTjRqRnl4UHMyRTBGZ0VDeXJwL0dhUVRBY0oxVWJveU13PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/clBMd0JTVUJ1L1diVUl6ZDFJRUVqRWoxWG1GdzlFeE56a3NPcVhTOHZyWlV1Q0NGN1JFZHFBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/clBMd0JTVUJ1L1dma3dtRmtIL1Z1WHUwZlBJYUZaQ05MRFI3Q1RMYWZUbzRndmFURGVraDR3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/clBMd0JTVUJ1L1VLRDZLbytjOFh5WkRDNW9wYzcvSHhqdW53WnNZUzAwRmhiUlo5NEJqcTd3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/clBMd0JTVUJ1L1hlNmVhNUVkNWhJR3VGcWRZeERYd0p3MkhCWExXelZ1aVorbWZlTm5NbDBnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/clBMd0JTVUJ1L1cyY3ZTVDZLUmFhaXVtNXY5amlobDNxSU82QllsQzBORzQraTQ2VDU0eDhnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/clBMd0JTVUJ1L1VBelNqQmx0OHl2Uk9RZmsyK254QnFYbmg0SlhJM054bVhwVnpBMVlLQWVnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf0.nosdn.127.net/img/clBMd0JTVUJ1L1V1L3VBSHZBTWk1VGxsQVZoMG1ZTnprMkYrbisrOTMvTkpQZFFxSFVtSzdBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/clBMd0JTVUJ1L1hMYkVJVDBnNHhCRERPY1hKQTlBOTFodzR3SWVGdytRZmJjR3IrQ29uaU5RPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/clBMd0JTVUJ1L1h5WlV3U0FxUGF4aUNOQlpCejZwVXZxekd0bWtEREY2WjI2YkVqL1hZWlpBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/clBMd0JTVUJ1L1VlanFEeTRMdVRMU2VsZzBtYWhPN2MyMXNxZmlBVzJlempGc01MdmYrYjlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/clBMd0JTVUJ1L1gzRmRuTDg3SUt4MXAyc3Q1L0N6b25xdXVhME1Mc3NGckU3ZzlPckp0bUt3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/clBMd0JTVUJ1L1dlSENSOGlwbXRJZ0dwQ0ZwYS92MkczY1pBQ0FTSnpWVk04VlpNY2MwZi9BPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/clBMd0JTVUJ1L1dqRWdqYi9IL2JhWk83dmtWdlUyaUlqRmQwbGRNRjR4RVY2eTRZSXBnTW1BPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/clBMd0JTVUJ1L1VDVlMyazlrQVJJY2hqdjJhZU1yL3o1VXd3L2Z3Z0cyM2ZrTENyNGZxeWl3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/Nzd1V3RpRVM5OVlHQnF4T0orQjdPVVNSTnY1S2ZJeVFBVWFSTWVEUnJNTldraHdqYXNhazV3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn.127.net/img/Y1lhQ3dXenNoQ3FMOWQ5YUMvaHNlV3g3S1pGNks0WEk3MC9mdno3bGlIK2E1OStHdzc3Zm9nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYlplbXUwaVk3VXNKSmcxQThSMXlFSHRqMXlpTVJRQ0s3R3VhWGY5MFVLZ3F3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmJFMFlWYTFFME1DalJyLysrUmlqWmpyK3lrSmNGM2FrT2RpMFQ4cnlBRGNBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYllGOXFwL0dhQWNkelRQWkFsTUlBTEpVM29CZUpaaDgyMU5lQWNLM01uV2ZBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYmFtYzhRZlNnR2xVVUsyOVZ2K1hZeVRwVXJBK29kSGtKUEJyYTJBMU02bE1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmFtYzhRZlNnR2xVY2swNmF2ZFNsS0gxOE54Q0ZITDNDMjhhZXZ3TVV5WC9RPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2VGtDNTNzaUZkWHUyUHptMHllOHVOdEs5NDN4a2hYeWx3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2Zk5DZlBZOXpwZFNWV245dis1T1ZEN1QxRitvaWVBcHF3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2Wm9zd3g0Ulg5aUxTSlZCdktmMnhiRHNQUTNCd05qdzZRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2UTNvemFqVFc2RlZ5dWxyMjZYZzRnbGwxVTFsVHRCNGh3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2WWlYVnBlK1hMNnJCb1JJa3MrUlNEK0x2VzRLcThDUWxRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2ZUZEYXlRR3ZoZzVNcXEvVGtnU2IxZ3VMUkNaLzlmOWpBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYlpPMU5nbE93Q3U1b3VOSk5XQ3lXVmtXRkZVOFhkaXUyRjNtQUNtWkdGUlRnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYllFQW5ZNUFpK1FXUHhWdzJYaFRFY2NOM092bEFjbkZBMjZtc2RDOCs2UVlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYlowdjByNG1YeER0ajZQNGFOaEt5WFNYMldPUWFtYyt4Mjh5c01ZSWRoREx3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYlludXRySU1NY2JFeDE1OVZSOTU4akJ2SXhqS1N0NDJXaG5ERyt0VjFDSjJnPT0.jpeg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmFkQ3BJdFlsU3JTbWgxdkV5M0V2U1pES0hENW9SbmJPWUJnejNsRWFDVWVBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYllvU25yQVJSWHpxRGQvWk43L0hXNWdUQUtZZGh2RjZMeDNQUjFaVzBqVTlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn.127.net/img/bXpDUVJEUHlOYllvU25yQVJSWHpxSlcxMDF1eGVKN1RJcVJlSHBMeGVTMmFWcHdWWURDQm1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYlpPanJZa3NrT2pCR2dGRHR2SUxGc2Fkb1F1amQwZVBueE4xb1BJWWo4dXlRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYlpPanJZa3NrT2pCS0tWUXFVMzZFOTB0bjZXZlVCV0dPWU81dGlzbHpRZEh3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYlpsMWg4ZnQ3QkRDcUUxVGExYkxMUGFkN2ovaWdIMWZVQVNHYlRDbzg0UHl3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYlpsMWg4ZnQ3QkRDcjBib1JPVERMSDh5ampKelFHWVpjUk10cmE2U2dnOStBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYmJpNzI5WkhVRHJneHJ4dy9hUEZ6TXhLZUFaSW9BTGI0OTh3cWpDTmVEWHBRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmE2ZzgwR1NZNGJOSmlTOEY3d0FUbjhML1pzZkxtL1MyZSt3M1l4bURqb1R3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmFUb0wvUHNYei9tK0dQbkZZTUtPajBNVlpDRjQwY1VrdXVpcXBDd1UxZTZBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYllac0NXc2dzZTd5NTFrODBnM1dyWlRTZUZRUTBYMm5KNnFqVmFlVlBRYXlBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmI5ZVlVTHNyMVdqYXRsNUZaemVNS3l0Tko2L3hZbGdHWnROT3hUWWV6bGdBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmF2Zzl1dkxxdGpicE1ONWd4WG14UUJxODhTSzdGejdZdkVOcjIvb3FlM1JBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJRVEtSeG84c3VWT1JWOUpNOXdkNmFXbnhkU20rODlnNlpxSkdJMVFCNWN3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmFhUG1ISzQxTy84K2VwdEx4VzJOZE03aThhWXloN3ZHcDUwYUZqTzdWN1Z3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/Z2ZCK0FLUlNBY0ZYaGVWRUdtYVdMdENKdTA1MXNPV2JxcEFqcklXMmtpUEg1SEIwazJGQ1hnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/eXpaTnFvNTh4N0tZVlFPN1VwM2pPMHFjM0JsM3RaNStDZE1IeHNmblk5YWxBZ1FuM3VwY1d3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/TEw2cTEwZ1NIRnRsdmlPczlGaGV1c1M1d0hraXNCQS94TWtCeUxzcXZXY3hxK0w4RXpPODVnPT0.jpg?imageView&thumbnail=640x0&quality=1&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/R0NDZ0hHUWg4UGlRcHhUSXlRcFhMdEhueEVxanp3c0w1SXJsSlptQnY0UEI5ZVpwQkVUL3pRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/R0NDZ0hHUWg4UGlRcHhUSXlRcFhMb0lmL205ZVFIS1lINWtEU2FmUEFORms5Z045bzRiMWtnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/R0NDZ0hHUWg4UGlRcHhUSXlRcFhMdmxVV2V6dlhmN0o2dWE2UTBGZkF6b3BpRWZWRmVHbi9RPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/R0NDZ0hHUWg4UGlRcHhUSXlRcFhMazMzeVpiWG1vb2V0SmJjN3V1RWFOa2FxbGlpRnFLZFdBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn.127.net/img/clBMd0JTVUJ1L1hsY3h0TGlMdS9VOGZ4cnBVYXVDVEFaUHNrSk9ENmRPN2g1ME9vemI2OG1BPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/RHRhY0VIQmlDcE1pTnRJMFp3S3dMWWNMMnVwQXJSQ2Q0aDMvYldVWUxLM2svNXRjVFlzYUZ3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/RHRhY0VIQmlDcE93TjVhMzdJUzAyVndrMXBZNUdMRGpwekpHQ1R6OTJ2bnRIY0dxRFlYQk1RPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf5.nosdn0.126.net/img/RHRhY0VIQmlDcFBzeVJYODZweDZaRndjQVFHWWd6S3hTM3QyVmV4b1A3OFpqeUVuUzBxSTJnPT0.png?imageView&thumbnail=640x0&quality=100&stripmeta=0",
            "http://imglf5.nosdn0.126.net/img/RHRhY0VIQmlDcFA5UzQvUFFUSVJRcmRyOFZzZm9SWmxKWlVwbzIybHE2M0o2TVcyZWhyUWpRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/RHRhY0VIQmlDcE1pTnRJMFp3S3dMWWNMMnVwQXJSQ2Q0aDMvYldVWUxLM2svNXRjVFlzYUZ3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/RHRhY0VIQmlDcFBqV1JCUVBla0duQmh3eWRMNlczQ1NiZVZWVWZQRzdFc3VSUkZNdFFmWUN3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/a1hXSmdpTmVrN1hLSlFBZllIdXpnYis0TEpLbTEwcnBIN1EwOGRFblpVV0xNc1VUZVRtRXhnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/a1hXSmdpTmVrN1gvN0REcEpCV2g2SkFLWkZudHFKOGFBZTdZREYrdFR4WDEwdDNrTmxUMmtRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf3.nosdn0.126.net/img/a1hXSmdpTmVrN1gvN0REcEpCV2g2T0E5Z0tSNzQ0Mkc5azdYMjR0UksxNXJ1OGpZL2hMbG1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf6.nosdn0.126.net/img/a1hXSmdpTmVrN1h5YTNEUDJpNTVJSWtPM3pYSFlBQnpoeWpMUm9KKzlkU2ZpLzV1UlMrMkRRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf5.nosdn0.126.net/img/UWt3dWdLcVBGdURVQmxvSGV1cHRadFdadWlubjErU2NTdldwKzBDblB0Y2xrTEIra0tTU2FBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            "http://imglf4.nosdn0.126.net/img/UWt3dWdLcVBGdUIwcisveFVaUGVxVjhLRHdFUlp5Wkt2NVM5VmJicTF3YmQ2VjhmVzhUaXp3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg"
    };
}
