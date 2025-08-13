# 数据库初始化

-- 创建库
create database if not exists core_api;

-- 切换库
use core_api;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    email        varchar(256) unique                    not null comment '邮箱',
    userAccount  varchar(256)                           null comment '账号',
    userPassword varchar(512) default '0be190adeb335ec3b7d8c983b13c2b83'          not null comment '密码',
    userName     varchar(256) default '无名'             null comment '用户昵称',
    userAvatar   varchar(1024) default 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png' null comment '用户头像',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    balance      bigint       default 100               not null comment '余额(积分)',
    accessKey    varchar(512)                           null comment 'accessKey',
    secretKey    varchar(512)                           null comment 'secretKey',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'
) comment '用户' collate = utf8mb4_unicode_ci;

-- 接口信息表
create table if not exists api_info
(
    id             bigint auto_increment comment 'id' primary key,
    userId         bigint                             not null comment '创建人',
    docId          bigint       default -1            null comment '文档 id，默认为 -1 表示未绑定文档',
    name           varchar(256)                       not null comment '名称',
    description    varchar(256) default 'none'        null comment '描述',
    openapiUrl     varchar(256)                      null comment 'OpenAPI 文档地址',
    host           varchar(256)                       not null comment '主机',
    port           varchar(32)                        null comment '端口',
    path           varchar(256)                       not null comment '路径',
    method         varchar(32)                       not null comment '请求方法',
    status         int      default 0                 not null comment '接口状态(0-关闭,1-开启)',
    cost           int      default 0                 not null comment '每次调用花费',
    queryExample   varchar(256)                       null comment '查询参数示例，例: key1=value1&key2=value2',
    reqBodyExample varchar(1024)                      null comment '请求体示例',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除(0-未删,1-已删)',
    index ids_name (name),
    index idx_pm (path, method),
    index idx_docId (docId)
) comment '接口信息' collate = utf8mb4_unicode_ci;