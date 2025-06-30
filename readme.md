# 1、项目名称
🚀 es数据同步至greenplum

# 2、项目简介
一个支持增量、动态配置的 ElasticSearch → Greenplum 数据同步工具。

# 3、项目环境
| 组件名称       | 版本号         |
|----------------|----------------|
| Elasticsearch  | 6.3.2          |
| Greenplum      | 9.4.24         |
| JDK            | 1.8.0_371      |

# 4、功能特性
- 功能 1：支持csv文件式同步加载
- 功能 2：支持动态增加index同步任务
- 功能 3：支持根据时间字段增量同步

# 5、快速开始

## 5.1、代码下载
```bash 
git clone https://github.com/marunjia/es_data_sync.git
```

## 5.2、进入目录
cd your_project

## 5.3、 代码打包
```bash
mvn clean package
```

## 5.4、 代码上传至服务器
具体上传路径根据自己的服务器确定,假定上传路径为：/opt/task

## 5.5、 创建配置文件
```bash
cd /opt/task
touch sync_index_list.config

#第1列为index，第2列为增量抽取的时间依赖字段，中间用|分割
vehicle_enter_exit_record|enterTime
vehicle_stay_point_record|enterTime
```

## 5.6、启动任务
```bash
#启动jar包任务
nohup java -jar es_data_sync-1.0-SNAPSHOT.jar "2025-09-30" > es_data_sync_20250630.log  2>&1 &

#查看任务运行日志
tail -f es_data_sync_20250630.log
```

## 5.7、 确认数据同步情况
- 登录gp数据库
- 查看对应数据表是否存在
- 查看数据表数据字段与数值是否匹配
- 查看数据表数据量是否一致

## 5.8、 追加同步任务
```bash
cd /opt/task
touch sync_index_list.config

#第1列为index，第2列为增量抽取的时间依赖字段，中间用|分割
vehicle_enter_exit_record|enterTime
vehicle_stay_point_record|enterTime
police_dept_info|createTime
```