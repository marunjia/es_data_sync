package util;

import java.io.InputStream;
import java.util.Properties;

/**
 * @ClassName ConfigUtils
 * @Description 配置文件加载工具类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/30 10:00:00
 * @Version 1.0
 */
public class ConfigUtils {
    private static final Properties p = new Properties();

    public static void load(String file) throws Exception {
        try (InputStream is = ConfigUtils.class.getClassLoader().getResourceAsStream(file)) {
            p.load(is);
        }
    }

    public static String get(String key) {
        return p.getProperty(key);
    }
}