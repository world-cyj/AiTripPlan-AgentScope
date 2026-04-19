package utils;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;

/**
 * author: Imooc
 * description: Nacos utility
 * date: 2026
 */
public class NacosUtil {

    private static final String DEFAULT_NACOS_SERVER_ADDR = "localhost:8848";

    public static AiService getNacosClient() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR,
                getOptionalValue("NACOS_SERVER_ADDR", DEFAULT_NACOS_SERVER_ADDR));
        return AiFactory.createAiService(properties);
    }

    private static String getOptionalValue(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
