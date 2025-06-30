package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;

/**
 * @ClassName EsUtils
 * @Description es操作工具类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/30 10:00:00
 * @Version 1.0
 */
public class EsUtils {
    private static final Logger log = LoggerFactory.getLogger(EsUtils.class);
    private static final RestHighLevelClient client;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(5));

    static {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(ConfigUtils.get("es.username"), ConfigUtils.get("es.password")));

        client = new RestHighLevelClient(
                RestClient.builder(
                                new HttpHost(ConfigUtils.get("es.host"),
                                        Integer.parseInt(ConfigUtils.get("es.port")),
                                        ConfigUtils.get("es.scheme")))
                        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }
                        })
        );
    }

    /**
     * 获取字段映射
     */
    public static Map<String, String> getFieldMapping(String index) {
        try {
            Response response = client.getLowLevelClient().performRequest("GET", "/" + index + "/_mapping");
            Map map = mapper.readValue(response.getEntity().getContent(), Map.class);

            Map indexMapping = (Map) map.get(index);
            Map mappings = (Map) indexMapping.get("mappings");

            // 判断是否存在 _doc 层（避免 NPE）
            Map docMapping;
            if (mappings.containsKey("_doc")) {
                docMapping = (Map) mappings.get("_doc");
            } else {
                // 支持没有 _doc 的扁平结构（某些ES版本可能没有 _doc）
                docMapping = mappings;
            }

            Map props = (Map) docMapping.get("properties");
            log.info(props.toString());

            Map<String, String> result = new LinkedHashMap<>();
            for (Object k : props.keySet()) {
                String key = (String) k;
                Map fieldInfo = (Map) props.get(key);
                String esType = (String) fieldInfo.get("type");
                String gpType = ConfigUtils.get("es.type.to.gp." + esType);
                result.put(key, gpType != null ? gpType : "TEXT");
            }
            return result;
        } catch (Exception e) {
            log.error("获取映射失败：" + index, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 滚动查询
     */
    public static SearchResponse scroll(String index, String date, String scrollId, String timeField) {
        try {
            if (scrollId == null) {
                // 初始查询
                SearchRequest searchRequest = new SearchRequest(index);
                searchRequest.scroll(scroll);

                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
                sourceBuilder.size(1000); // 每次返回 1000 条
                sourceBuilder.query(QueryBuilders.termQuery(timeField, date)); //根据指定的时间进行过滤
                searchRequest.source(sourceBuilder);

                return client.search(searchRequest);
            } else {
                // 后续滚动查询
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                return client.searchScroll(scrollRequest);
            }
        } catch (IOException e) {
            log.error("Scroll 查询失败：index=" + index + ", scrollId=" + scrollId, e);
            return null;
        }
    }

    /**
     * 关闭客户端
     */
    public static void close() throws IOException {
        client.close();
    }
}