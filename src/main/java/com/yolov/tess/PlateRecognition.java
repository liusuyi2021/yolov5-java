package com.yolov.tess;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;
import java.io.IOException;

/**
 * @ClassName:PlateRecognition
 * @Description: 车牌识别demo
 * @Author:ard
 * @Date:2024年05月24日15:02
 * @Version:1.0
 **/
public class PlateRecognition {
    public static String path = "E:\\yolov\\yolov5\\runs\\detect\\exp3\\crops\\plate\\0.jpg";
    public static String TESSDATA = "E:\\tess\\tessdata";

    public static void main(String[] args) throws IOException {
        ITesseract instance = new Tesseract();
        instance.setDatapath(TESSDATA);
        instance.setLanguage("chi_sim"); // 设置中文语言
        try {
            String result = instance.doOCR(new File(path));
            System.out.println(result);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
