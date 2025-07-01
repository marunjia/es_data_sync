package util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * @ClassName CsvUtils
 * @Description 数据生成csv文件工具类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/30 10:00:00
 * @Version 1.1
 */
public class CsvUtils {
    private static final Logger log = LoggerFactory.getLogger(CsvUtils.class);

    /**
     * 将ES数据写入CSV文件
     */
    public static File writeEsDataToCsv(String idx, String date, Map<String, String> fields, String timeField) throws Exception {
        File f = new File(idx + "_" + date + ".csv");
        if (f.exists()) {
            f.delete();
        }

        try (FileWriter w = new FileWriter(f);
             CSVPrinter printer = new CSVPrinter(w, CSVFormat.DEFAULT.withHeader(fields.keySet().toArray(new String[0])))) {

            String scrollId = null;
            SearchResponse resp;

            do {
                resp = EsUtils.scroll(idx, date, scrollId, timeField);
                scrollId = resp.getScrollId();

                SearchHit[] hits = resp.getHits().getHits();
                for (SearchHit hit : hits) {
                    Map<String, Object> src = hit.getSourceAsMap();
                    Object[] record = fields.keySet().stream()
                            .map(k -> src.getOrDefault(k, ""))
                            .toArray();
                    printer.printRecord(record);
                }

                log.info("写入[{}] {} 条", idx, hits.length);

            } while (resp.getHits().getHits().length > 0);
        }

        return f;
    }

    /**
     * 删除指定CSV文件
     */
    public static boolean deleteCsvFile(File file) {
        String fileName = file.getName();
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("已删除CSV文件：{}", fileName);
            } else {
                log.warn("删除CSV文件失败：{}", fileName);
            }
            return deleted;
        } else {
            log.warn("CSV文件不存在：{}", fileName);
            return false;
        }
    }
}