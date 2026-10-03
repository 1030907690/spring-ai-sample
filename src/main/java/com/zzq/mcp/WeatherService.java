package com.zzq.mcp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: Zhou Zhongqing
 * @date: 10/3/2026 3:22 PM
 */
@Service
public class WeatherService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        logger.info("Get weather information by city name {}", cityName);
        return "30";
    }
}
