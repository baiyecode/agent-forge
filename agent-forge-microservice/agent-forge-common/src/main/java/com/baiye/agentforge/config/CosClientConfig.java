package com.baiye.agentforge.config;



import com.qcloud.cos.COSClient;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.ClientConfig;


/**
 * ClassName: CosClientConfig
 * Package: com.baiye.agentforge.config
 * Description: 腾讯云COS配置类
 *
 * @Author 白夜
 * @Create 2026/5/19 16:33
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@ConditionalOnProperty( // 当配置文件中存在cos.client时，才会加载该配置类
        prefix = "cos.client",
        name = {"host", "secretId", "secretKey", "region", "bucket"}
)
@Data
public class CosClientConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * secretId
     */
    private String secretId;

    /**
     * 密钥（注意不要泄露）
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private String bucket;

    @Bean
    public COSClient cosClient() {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }
}

