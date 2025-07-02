package code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName DataSync
 * @Description 数据同步(es to greenplumn)
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/30 10:00:00
 * @Version 1.0
 */
public class DataSync {

    private static final Logger log = LoggerFactory.getLogger(DataSync.class);

    public static void main(String[] args) throws Exception {
        /**
         * 日期参数处理
         */
        if (args.length < 1) {
            log.error("请传入日期参数，格式为：yyyy-MM-dd");
            return;
        }
        String date = args[0];

        //加载配置文件
        ConfigUtils.load("config.properties");

        // 读取 index -> 时间字段 映射
        Map<String, String> indexTimeFieldMap = Files.readAllLines(Paths.get(ConfigUtils.get("index_list_file")))
                .stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\|"))  // 空格或Tab分隔
                .filter(arr -> arr.length >= 2)
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

        log.info("索引和时间字段读取完成，共 {} 个索引", indexTimeFieldMap.size());

        String year = date.substring(0, 4);
        String month = date.substring(5, 7);

        /**
         * 遍历索引列表同步数据，同步流程如下：
         *  1、检测索引对应数据结构
         *  2、检测gp对应数据表是否存在，如不存在则自动创建
         *  3、es读取数据写入csv文件
         *      3.1、如果读取日期为月末，则需要扫描2个index，对应本月月末与下月月初
         *      3.2、如果读取日期非月末，则需要扫描1个index，对应当前日期即可
         *  4、gp加载csv文件到对应数据表
         */
        for (Map.Entry<String, String> entry : indexTimeFieldMap.entrySet()) {
            String idx = entry.getKey().concat("_").concat(year).concat(".").concat(month).concat("_m");//上游数据源每月生成一个index，所以后缀格式为：tableName_年份.月份_m
            String timeField = entry.getValue();
            String tableName = entry.getKey();

            log.info("处理索引:{}，同步数据日期:{}，增量同步依赖时间字段:{}", idx, date, timeField);

            Map<String, String> mapping = EsUtils.getFieldMapping(idx);
            if (mapping.isEmpty()) {
                log.warn("跳过空结构索引：{}", idx);
                continue;
            }

            if (!GreenplumUtils.tableExists(tableName)) {
                GreenplumUtils.createTable(tableName, mapping);
            }

            //从当前日期所属月份对应的index读取数据
            File csvCurrentMonth = CsvUtils.writeEsDataToCsv(idx, date, mapping,timeField);//读取数据生成csv文件临时存储
            GreenplumUtils.copyCsvToGp(tableName, csvCurrentMonth);//csv文件入库
            CsvUtils.deleteCsvFile(csvCurrentMonth);//删除csv文件

            //月末最后一天，抽取下月index中对应的本月月末数据
            if(DateUtils.isMonthLastDay(date)){
                String nextDay = DateUtils.getNextDay(date);
                String nextDayOfYear = nextDay.substring(0,4); //获取明日对应年份
                String nextDayOfMonth = nextDay.substring(5,7); //获取明日对应月份
                idx = entry.getKey().concat("_").concat(nextDayOfYear).concat(".").concat(nextDayOfMonth).concat("_m");//上游数据源每月生成一个index，所以后缀格式为：tableName_年份.月份_m
                log.info("月末最后一天：{}，从下月index获取数据，index：{}",date,idx);
                File csvNextDay = CsvUtils.writeEsDataToCsv(idx, date, mapping,timeField);//读取数据生成csv文件临时存储
                GreenplumUtils.copyCsvToGp(tableName, csvNextDay);//csv文件入库
                CsvUtils.deleteCsvFile(csvNextDay);//删除csv文件
            }
        }

        EsUtils.close();
        log.info("所有索引处理完毕");
    }
}