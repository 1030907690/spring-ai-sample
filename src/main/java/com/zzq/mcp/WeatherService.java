package com.zzq.mcp;


import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: Zhou Zhongqing
 * @date: 10/3/2026 3:22 PM
 */
@Service
public class WeatherService {

    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        return "30";
    }
}
