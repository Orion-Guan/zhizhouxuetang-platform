package com.tianji.learning.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.tianji.learning.utils.TableNameContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamicTableNameConfig {

    @Bean
    public DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor(){
        DynamicTableNameInnerInterceptor tableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
        tableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> {
            // 只替换 points_board 表，其他表保持原样
            if ("points_board".equals(tableName)) {
                String newTableName = TableNameContext.getInfo();
                if (StrUtil.isNotBlank(newTableName)) {
                    return newTableName;
                }
            }
            return tableName;
        });
        return tableNameInnerInterceptor;
    }
}
