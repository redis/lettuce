package org.mybatis.spring.annotation.mapper.ds1;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan
@MapperScan("org.mybatis.spring.annotation.mapper.ds2")
public class AppConfigWithDefaultMapperScanAndRepeat {
}
