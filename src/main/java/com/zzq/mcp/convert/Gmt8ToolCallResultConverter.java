package com.zzq.mcp.convert;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.util.json.JsonParser;

import java.lang.reflect.Type;
import java.util.TimeZone;

/**
 * 转东八区时间
 * @author: Zhou Zhongqing
 * @date: 10/3/2026 3:34 PM
 */

public class Gmt8ToolCallResultConverter implements ToolCallResultConverter {

    private final ObjectMapper GMT8_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModules(JacksonUtils.instantiateAvailableModules())
            .defaultTimeZone(TimeZone.getTimeZone("GMT+8"))
            .build();

    @Override
    public String convert(Object result, Type returnType) {
        if (returnType == Void.TYPE) {
            return JsonParser.toJson("Done");
        }
        try {
            return GMT8_MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }
}