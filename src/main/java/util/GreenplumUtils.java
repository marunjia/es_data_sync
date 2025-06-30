package util;

import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;

/**
 * @ClassName GreenplumUtils
 * @Description gp操作工具类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/30 10:00:00
 * @Version 1.0
 */
public class GreenplumUtils {
    private static final Logger log = LoggerFactory.getLogger(GreenplumUtils.class);

    /**
     * 获取数据库连接
     */
    private static Connection conn() throws SQLException {
        return DriverManager.getConnection(
                ConfigUtils.get("gp.url"),
                ConfigUtils.get("gp.user"),
                ConfigUtils.get("gp.password")
        );
    }

    /**
     * 获取带 schema 的完整表名
     */
    private static String getFullTableName(String tbl) {
        String schema = ConfigUtils.get("gp.schema"); // 从配置读取 gp.schema
        return "\"" + schema + "\".\"" + tbl + "\""; // 加引号防止大小写或关键词冲突
    }

    /**
     * 判断表是否存在
     */
    public static boolean tableExists(String tbl) throws SQLException {
        Connection c = null;
        ResultSet rs = null;
        try {
            c = conn();
            String schema = ConfigUtils.get("gp.schema");
            rs = c.getMetaData().getTables(null, schema, tbl, null);
            boolean exists = rs.next();
            log.info("表 {}.{} 是否存在: {}", schema, tbl, exists);
            return exists;
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (c != null) try { c.close(); } catch (Exception e) {}
        }
    }

    /**
     * 创建表
     */
    public static void createTable(String tbl, Map<String, String> fields) throws SQLException {
        String fullTbl = getFullTableName(tbl);
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            sb.append("\"").append(entry.getKey()).append("\" ").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }

        Connection c = null;
        Statement s = null;
        try {
            c = conn();
            s = c.createStatement();
            s.execute("CREATE TABLE " + fullTbl + " (" + sb + ")");
            log.info("已创建表 {}", fullTbl);
        } finally {
            if (s != null) try { s.close(); } catch (Exception e) {}
            if (c != null) try { c.close(); } catch (Exception e) {}
        }
    }

    /**
     * 将CSV写入Greenplum表
     */
    public static void copyCsvToGp(String tbl, File csv) throws SQLException {
        String fullTbl = getFullTableName(tbl);
        Connection c = null;
        FileReader fr = null;
        try {
            c = conn();
            PGConnection pg = c.unwrap(PGConnection.class);
            fr = new FileReader(csv);
            pg.getCopyAPI().copyIn("COPY " + fullTbl + " FROM STDIN WITH CSV HEADER", fr);
            log.info("已写入 Greenplum 表 {} 文件 {}", fullTbl, csv.getName());
        } catch (Exception e) {
            log.error("写入失败", e);
            throw new SQLException(e);
        } finally {
            if (fr != null) try { fr.close(); } catch (Exception e) {}
            if (c != null) try { c.close(); } catch (Exception e) {}
        }
    }
}