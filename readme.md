#功能描述
es数据同步到greenplum

#支持能力
配置化导入es表，导入信息包括es index

#实现流程

1、从外部读取index配置文件,配置文件每行代表一个index

2、逐行读取配置文件
    获取index名称
    获取index对应的表结构；
    检查greenplum表是否创建对应表结构；如果没有表结构创建greenplum数据表；
    分页读取index数据；
    批量导入csv文件到greenplum；
3、