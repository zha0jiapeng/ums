package com.global.ums.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 二维码工具类
 */
@Slf4j
public class QRCodeUtil {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final String FORMAT = "png";
    private static final String CHARSET = "utf-8";

    /**
     * 生成随机场景ID
     */
    public static String generateSceneId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成二维码图片Base64字符串
     *
     * @param content 二维码内容
     * @param width   宽度
     * @param height  高度
     * @return Base64编码的二维码图片
     */
    public static String createQRCodeBase64(String content, int width, int height) {
        try {
            BitMatrix bitMatrix = createQRCodeBitMatrix(content, width, height);
            BufferedImage image = toBufferedImage(bitMatrix);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(image, FORMAT, outputStream);
                byte[] bytes = outputStream.toByteArray();
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (WriterException | IOException e) {
            log.error("生成二维码失败", e);
        }
        return null;
    }

    /**
     * 创建二维码位矩阵
     */
    private static BitMatrix createQRCodeBitMatrix(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // 设置二维码纠错级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 设置编码
        hints.put(EncodeHintType.CHARACTER_SET, CHARSET);
        // 设置边距
        hints.put(EncodeHintType.MARGIN, 1);
        
        return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    }

    /**
     * 将位矩阵转为图片
     */
    private static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }
        
        return image;
    }
} 