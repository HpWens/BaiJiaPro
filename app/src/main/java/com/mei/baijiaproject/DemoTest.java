package com.mei.baijiaproject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/8/1
 */
public class DemoTest {

    public static void main(String[] args) {

        String input = "<!DOCTYPE html><html><head><link><script></script><meta><meta><title>【忘羡】《千疼百宠》37-38-正襟危坐的炕</title><link><link><meta><meta><link></head><iframe></iframe><iframe></iframe><body><div>    <div>                                    UAPP                            私信                        归档        RSS    </div>            <div>                <div><div><p>上一篇</p></div></div>        <div><div><span>下一篇</span></div></div>            </div>        <div>        <h1>正襟危坐的炕</h1>        <p>【请点开】微博id正襟危坐的炕<br><br>／在不越线的情况下还是很温和的<br>说一不二极端冷漠专制主义者／</p>    </div></div><div>            <div>                <div><p>上一篇</p></div>        <div><span>下一篇</span></div>            </div>        <div>        <h1>正襟危坐的炕</h1>    </div></div><div>\t<div>    \t                                                                                                            <div>                            <div>                                <div>                                    <h2>【忘羡】《千疼百宠》37-38</h2>                                    <p>《千疼百宠》37-38</p> <p>-原著向，些许魔改</p> <p>-（差不多等同于穿过去的婚后）熟男叽x老祖羡</p> ";

        Pattern proInfo
                = Pattern.compile("<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>\\s*<div>(.*?)</div>", Pattern.DOTALL);

        Matcher m = proInfo.matcher(input.replaceAll(" ", ""));

        if (m.find()) {
            System.out.println("" + m.group().trim().length());
        }

        System.out.println("" + input.substring(input.indexOf("<h2>")));
    }
}
