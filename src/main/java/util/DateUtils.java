package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName DateUtil
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/1 20:05
 * @Version 1.0
 */
public class DateUtils {

    /**
     * @desc 判断输入日期是否为月末最后一天
     * @param inputDate
     * @return
     */
    public static boolean isMonthLastDay(String inputDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(inputDate, formatter);

        // 判断是否是月末
        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            return true;
        }else{
            return false;
        }
    }

    /**
     * @desc 返回日期最后一天
     * @param inputDate
     * @return 下个月第一天（如满足条件），否则返回 null
     */
    public static String getNextDay(String inputDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(inputDate, formatter);
        LocalDate nextMonthFirstDay = date.plusDays(1);
        return nextMonthFirstDay.format(formatter);
    }
}