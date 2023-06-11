package cc.cornerstones.almond.utils;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AbcCompressUtils {
    /**
     * 需要压缩的原始字符串长度阈值
     */
    public static final Integer STRING_COMPRESS_LENGTH_THRESHOLD = 1000;

    private AbcCompressUtils() {

    }

    /**
     * 用gzip压缩输入文本，输出base64编码文本
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static String compress(String input) throws IOException {
        if (input == null || input.length() == 0) {
            return input;
        }

        ByteArrayOutputStream out = null;
        GZIPOutputStream gzip = null;
        try {
            out = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(out);
            gzip.write(input.getBytes(StandardCharsets.UTF_8));
            gzip.close();
            return Base64.encodeBase64String(out.toByteArray());
        } catch (IOException e) {
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
            if (gzip != null) {
                gzip.close();
            }
        }
    }

    /**
     * 用gzip解压输入base64编码文本
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static String decompress(String input) throws IOException {
        if (input == null || input.length() == 0) {
            return input;
        }

        byte[] bytes = Base64.decodeBase64(input);

        InputStream in = null;
        ByteArrayOutputStream baos = null;
        try {
            in = new GZIPInputStream(new ByteArrayInputStream(bytes));
            baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return new String(baos.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
    }

}
