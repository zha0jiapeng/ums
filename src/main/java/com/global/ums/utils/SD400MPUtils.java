package com.global.ums.utils;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.global.ums.properties.GlbProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SD400MPUtils {
    private static final String URI = GlbProperties.uri;
    private static final String USERNAME = GlbProperties.username;
    private static final String PASSWORD = GlbProperties.password;
    
    
    /**
     * 获取认证令牌
     * 首先从Redis缓存中获取，如果不存在则通过API请求新的令牌
     * 
     * @return 认证令牌字符串
     */
    public static String getToken() {
        // // 从缓存中获取令牌
        //  Object glbToken = RedisUtil.get(TOKEN_CACHE_KEY);
        //  if (glbToken != null) {
        //      return glbToken.toString();
        //  }
        
        Map<String, Object> requestMap = new HashMap<>(2);
        requestMap.put("user", USERNAME==null?"admin":USERNAME);
        requestMap.put("password", PASSWORD==null?"Pd700@sdmt":PASSWORD);
        
        try {
            // 发送HTTP请求获取令牌
            HttpResponse response = HttpUtil.createPost("https://192.168.110.11:40080/api/auth")
                    .contentType("application/json")
                    .body(JSON.toJSONString(requestMap))
                    .execute();
            
            String body = response.body();
            if (body == null || body.isEmpty()) {
                throw new RuntimeException("获取GLB令牌失败:响应内容为空");
            }
            
            // 解析响应获取令牌
            Map<String, Object> tokenResult = JSON.parseObject(body, Map.class);
            if (tokenResult == null || !tokenResult.containsKey("token")) {
                throw new RuntimeException("获取GLB令牌失败:响应格式错误");
            }
            
            Object token = tokenResult.get("token");
            if (token == null) {
                throw new RuntimeException("获取GLB令牌失败:令牌为空");
            }
            
            // 将令牌存入缓存
           // RedisUtil.set(TOKEN_CACHE_KEY, token, 10, TimeUnit.MINUTES);
            return token.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取GLB令牌失败:" + e.getMessage(), e);
        }
    }

    /**
     * 根据时间范围查询索引信息
     * @param uniqueId 唯一标识
     * @param from 开始时间
     * @param to 结束时间
     * @return 包含id和times的Map,查询失败返回null
     */
    public static JSONObject index(String uniqueId, String from, String to) {
        try {
            // 构建请求参数
            Map<String, Object> requestMap = new HashMap<>(2);
            requestMap.put("token", getToken());
            
            Map<String, Object> dataMap = new HashMap<>(4);
            dataMap.put("uniqueId", uniqueId);
            dataMap.put("from", from); 
            dataMap.put("to", to);
            requestMap.put("data", dataMap);

            // 发送请求
            String responseBody = HttpUtil.createPost(URI + "/api/index")
                    .body(JSON.toJSONString(requestMap),ContentType.JSON.toString())
                    .execute()
                    .body();

            // 解析响应
            JSONObject response = JSONObject.parseObject(responseBody);
            if (response == null) {
                log.error("请求索引接口返回数据为空");
                return null;
            }

            Integer code = response.getInteger("code");
            if (code == 200) {
//                 Map<String, Object> data = response.getObject("data", Map.class);
//                // Map<String, Object> idResult = (Map<String, Object>) data.get("id");
//
//                // Map<String, Object> result = new HashMap<>(2);
//                // result.put("id", idResult.get("id"));
//                // result.put("times", data.get("time"));
//                System.out.println(data.get("time"));
                return response;
            } else {
                log.warn("查询索引失败 - uniqueId:{}, from:{}, to:{}, response:{}", 
                    uniqueId, from, to, response.toJSONString());
                return null;
            }
        } catch (Exception e) {
            log.error("查询索引异常 - uniqueId:{}, from:{}, to:{}", uniqueId, from, to, e);
            return null;
        }
    }

    /**
     * 获取数据集
     * @param id 数据ID
     * @param time 时间
     * @return HTTP响应
     * @throws IOException IO异常
     */
    public static byte[] dataset(Object id, String time) throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        
        Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("id", id);
        dataMap.put("time", time);
        map.put("data", dataMap);

        HttpResponse execute = HttpUtil.createPost(URI + "/api/dataset")
                        .body(JSON.toJSONString(map),ContentType.JSON.toString())
                        .execute();
        if(execute.getStatus() == 200 && execute.header(HttpHeaders.CONTENT_TYPE).contains(ContentType.OCTET_STREAM.getValue())){
            return execute.bodyBytes();
        }
        return null;
    }
    
    /**
     * 上传文件到PDExpert
     * @param file 要上传的文件
     * @return HTTP响应
     */
    public static HttpResponse pdexpert(File file) {
        Map<String, Object> map = new HashMap<>(2);
       // map.put("token", getToken());
        map.put("file", file);
        
        return HttpUtil.createPost(URI + "/api/pdexpert")
                .form(map)
                .execute();
    }
    /**
     * 上传PDES文件
     * @param file 要上传的文件
     * @return HTTP响应
     */
    public static HttpResponse uploadPdesFile(File file) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        map.put("file", file);
        
        return HttpUtil.createPost(URI + "/api/pdexpertUpdate")
                .form(map)
                .execute();
    }

    public static JSONObject equipmentList(String equipmentId,boolean flag) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(4);
        if(equipmentId != null){
            dataMap.put("id", equipmentId);
        }
         dataMap.put("needChildren", flag);
        // dataMap.put("needAllData", true);
        // dataMap.put("fullscope", false);
        map.put("data",dataMap);

        String body = HttpUtil
                .createPost(URI + "/api/equipment")
                .body(JSON.toJSONString(map))
                .execute().body();
        return JSONObject.parseObject(body);

    }


    public static JSONObject testPointList(String equipmentId) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("token", getToken());
        Map<String, Object> dataMap = new HashMap<>(4);
        dataMap.put("id", equipmentId);
        dataMap.put("needChildren", false);
        dataMap.put("needAllData", true);
        dataMap.put("fullscope", false);
        map.put("data",dataMap);
        System.out.println(JSON.toJSONString(map));
        String body = HttpUtil.createPost(URI + "/api/testpoint")
                .body(JSON.toJSONString(map))
                .execute().body();
        return JSONObject.parseObject(body);
    }

    /**
     * 位置比较
     * @param requestData 比较请求数据
     * @return HTTP响应
     */
    public static JSONObject locationCompare(Map<String,Object> requestData) {
        String body = HttpUtil.createPost(URI + "/api/location/compare")
                .body(JSON.toJSONString(requestData))
                .execute().body();
        return JSONObject.parseObject(body);
    }

    /**
     * 报告
     * @param testpointId 测点id
     * @return HTTP响应
     */
    public static JSONObject reportCurrent(String testpointId) {
        Map<String,Object> requestData = new HashMap<>();
        requestData.put("token", getToken());
        Map<String,Object> dataMap = new HashMap<>();
        dataMap.put("id", testpointId);
        dataMap.put("culture", "zh");
        dataMap.put("abnormal", false);
        dataMap.put("html", true);
        requestData.put("data", dataMap);
        String body = HttpUtil.createPost(URI + "/api/report/current")
                .body(JSON.toJSONString(requestData))
                .execute().body();
        return JSONObject.parseObject(body);
    }

    public static JSONObject archive() {
        Map<String,Object> requestData = new HashMap<>();
        requestData.put("token", getToken());
        Map<String,Object> dataMap = new HashMap<>();
        dataMap.put("type", "1");
        dataMap.put("from", "2024-01-27T11:51:48.217+08:00");
        dataMap.put("to", "2024-06-26T11:51:48.217+08:00");
        dataMap.put("id", "1009");
        requestData.put("data", dataMap);
        String body = HttpUtil.createPost("https://192.168.110.11:40080/api/archive")
                .body(JSON.toJSONString(requestData))
                .execute().body();
        return JSONObject.parseObject(body);
    }
    
}
