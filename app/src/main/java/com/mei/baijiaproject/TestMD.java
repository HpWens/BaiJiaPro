package com.mei.baijiaproject;

import java.io.BufferedReader;
import java.io.File;
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

    private static String testUrl = "http://juexixi.lofter.com/post/1d3d4306_1c5f5791c?sharefrom=lofter-android-6.2.5&shareto=qq";

    private static int line = 0;
    private static boolean startParse = false;

    private static List<String> results = new ArrayList<>();

    // 添加的图片张数
    private static int picTotal = 6;

    private static String[] picIndexArray = new String[picTotal];

    private static List<String> picList = new ArrayList<>();

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                picList = new ArrayList<>();
                for (String s : pics) {
                    picList.add(s);
                }
                getURLInfo(testUrl);
            }
        }).start();
    }

    private static void getPicArray() {
        for (int i = 0; i < picTotal; i++) {
            getPicArrayIndex(i);
        }
    }

    private static void getPicArrayIndex(int i) {
        int i0 = new Random().nextInt(picList.size());
        picIndexArray[i] = picList.get(i0);
        picList.remove(i0);
    }

    static Pattern proInfo
            = Pattern.compile("<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>", Pattern.DOTALL);

    public static void getURLInfo(String addressUrl) {
        results = new ArrayList<>();
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
                            if (p.startsWith("<p><strong>") || p.startsWith("<div>")) {
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

                for (int j = 0; j < 3; j++) {
                    int ss = results.size() / 3;
                    List<String> listMd = results.subList(j * ss, j == 2 ? results.size() : ((j + 1) * ss));
                    getPicArray();

                    int size = listMd.size();
                    int count = size / (picTotal + 1);
                    for (int i = 0; i < picTotal; i++) {
                        listMd.add(count + i * count + (results.isEmpty() ? 0 : new Random().nextInt(3)),
                                "\n" + "### " + translateNumToMan(i) + "\n" +
                                        "![](" + picIndexArray[i] + ")");
                    }

                    File file = new File("D:\\0" + j + ".md");
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    StringBuilder ssb = new StringBuilder();
                    int partIndex = 0;
                    ssb.append("### 道友好久不见，甚是想念，别说话，吻我~~，小爱等着你哟 \n\n");
                    for (String s : listMd) {
                        partIndex++;
                        ssb.append(s + (partIndex % 2 == 0 ? "\n\n" : ""));
                    }
                    ssb.append("\n\n ### 小爱专注《魔道祖师》番外，想看更多精彩，脑洞打开的番外，请点击上方关注小爱~~");

                    if (file.isFile()) {
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(ssb.toString());
                        fileWriter.flush();
                        fileWriter.close();
                    }

                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String translateNumToMan(int num) {
        switch (num) {
            case 0:
                return "引人入胜";
            case 1:
                return "跌宕起伏";
            case 2:
                return "高潮渐起";
            case 3:
                return "一波三折";
            case 4:
                return "高潮再起";
            case 5:
                return "未完待续";
        }
        return "道友请留步";
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
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYlplbXUwaVk3VXNKSmcxQThSMXlFSHRqMXlpTVJRQ0s3R3VhWGY5MFVLZ3F3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmJFMFlWYTFFME1DalJyLysrUmlqWmpyK3lrSmNGM2FrT2RpMFQ4cnlBRGNBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYllGOXFwL0dhQWNkelRQWkFsTUlBTEpVM29CZUpaaDgyMU5lQWNLM01uV2ZBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYmFtYzhRZlNnR2xVVUsyOVZ2K1hZeVRwVXJBK29kSGtKUEJyYTJBMU02bE1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmFtYzhRZlNnR2xVY2swNmF2ZFNsS0gxOE54Q0ZITDNDMjhhZXZ3TVV5WC9RPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2VGtDNTNzaUZkWHUyUHptMHllOHVOdEs5NDN4a2hYeWx3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2Zk5DZlBZOXpwZFNWV245dis1T1ZEN1QxRitvaWVBcHF3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2Wm9zd3g0Ulg5aUxTSlZCdktmMnhiRHNQUTNCd05qdzZRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2UTNvemFqVFc2RlZ5dWxyMjZYZzRnbGwxVTFsVHRCNGh3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2WWlYVnBlK1hMNnJCb1JJa3MrUlNEK0x2VzRLcThDUWxRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn.127.net/img/bXpDUVJEUHlOYmJrTkE0Y2FLQXc2ZUZEYXlRR3ZoZzVNcXEvVGtnU2IxZ3VMUkNaLzlmOWpBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYlpPMU5nbE93Q3U1b3VOSk5XQ3lXVmtXRkZVOFhkaXUyRjNtQUNtWkdGUlRnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYllFQW5ZNUFpK1FXUHhWdzJYaFRFY2NOM092bEFjbkZBMjZtc2RDOCs2UVlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYlowdjByNG1YeER0ajZQNGFOaEt5WFNYMldPUWFtYyt4Mjh5c01ZSWRoREx3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYlludXRySU1NY2JFeDE1OVZSOTU4akJ2SXhqS1N0NDJXaG5ERyt0VjFDSjJnPT0.jpeg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmFkQ3BJdFlsU3JTbWgxdkV5M0V2U1pES0hENW9SbmJPWUJnejNsRWFDVWVBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYllvU25yQVJSWHpxRGQvWk43L0hXNWdUQUtZZGh2RjZMeDNQUjFaVzBqVTlnPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn.127.net/img/bXpDUVJEUHlOYllvU25yQVJSWHpxSlcxMDF1eGVKN1RJcVJlSHBMeGVTMmFWcHdWWURDQm1nPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYlpPanJZa3NrT2pCR2dGRHR2SUxGc2Fkb1F1amQwZVBueE4xb1BJWWo4dXlRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYlpPanJZa3NrT2pCS0tWUXFVMzZFOTB0bjZXZlVCV0dPWU81dGlzbHpRZEh3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf5.nosdn0.126.net/img/bXpDUVJEUHlOYlpsMWg4ZnQ3QkRDcUUxVGExYkxMUGFkN2ovaWdIMWZVQVNHYlRDbzg0UHl3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYlpsMWg4ZnQ3QkRDcjBib1JPVERMSDh5ampKelFHWVpjUk10cmE2U2dnOStBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYmJpNzI5WkhVRHJneHJ4dy9hUEZ6TXhLZUFaSW9BTGI0OTh3cWpDTmVEWHBRPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmE2ZzgwR1NZNGJOSmlTOEY3d0FUbjhML1pzZkxtL1MyZSt3M1l4bURqb1R3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmFUb0wvUHNYei9tK0dQbkZZTUtPajBNVlpDRjQwY1VrdXVpcXBDd1UxZTZBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn0.126.net/img/bXpDUVJEUHlOYllac0NXc2dzZTd5NTFrODBnM1dyWlRTZUZRUTBYMm5KNnFqVmFlVlBRYXlBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmI5ZVlVTHNyMVdqYXRsNUZaemVNS3l0Tko2L3hZbGdHWnROT3hUWWV6bGdBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf6.nosdn0.126.net/img/bXpDUVJEUHlOYmF2Zzl1dkxxdGpicE1ONWd4WG14UUJxODhTSzdGejdZdkVOcjIvb3FlM1JBPT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf4.nosdn.127.net/img/bXpDUVJEUHlOYmJRVEtSeG84c3VWT1JWOUpNOXdkNmFXbnhkU20rODlnNlpxSkdJMVFCNWN3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
            " http://imglf3.nosdn0.126.net/img/bXpDUVJEUHlOYmFhUG1ISzQxTy84K2VwdEx4VzJOZE03aThhWXloN3ZHcDUwYUZqTzdWN1Z3PT0.jpg?imageView&thumbnail=640x0&quality=100&stripmeta=0&type=jpg",
    };
}
