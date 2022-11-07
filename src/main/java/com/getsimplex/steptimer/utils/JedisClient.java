//© 2021 Sean Murdock

package com.getsimplex.steptimer.utils;


import com.typesafe.config.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Admin on 8/18/2016.
 */
public class JedisClient {

    private static Config config;
    private static String password;
    private static String host;
    private static String port;
    private static String dbName;
    private static String url;
    private static JedisPool jedisPool;

    static {
        Config config = Configuration.getConfiguration();
        if (System.getenv("REDIS_HOST")!=null && !System.getenv("REDIS_HOST").isEmpty()){

            password = System.getenv("REDIS_PASSWORD");
            host = System.getenv("REDIS_HOST");
            port = System.getenv("REDIS_PORT");
            dbName = System.getenv("REDIS_DB");
            if (password !=null && !password.isEmpty()){
                url = "redis://:"+password+"@"+host+":"+port+"/"+dbName;
                jedisPool  = new JedisPool(url);
            }
            else{
                jedisPool = new JedisPool(host, Integer.valueOf(port));
            }

        } else{
            config = Configuration.getConfiguration();
            try {
                password = config.getString("redis.password");
            } catch (Exception e){
                //config object throws exception for non-existent keys, workaround is to catch the exception
            }
            host = config.getString("redis.host");
            port = config.getString("redis.port");
            dbName = config.getString("redis.db");
            if (password !=null && !password.isEmpty()){
                url = "redis://:"+password+"@"+host+":"+port+"/"+dbName;
                jedisPool  = new JedisPool(url);
            } else{
                jedisPool = new JedisPool(host, Integer.valueOf(port));
            }

        }

    }

//    private static synchronized Jedis getJedis(){
//        Jedis jedis = jedisPool.getResource();
//        try{
//            jedis.ping();
//        }
//
//        catch (Exception e){
//            jedis = new Jedis(url);
//        }
//        return jedis;
//    }

    public static synchronized void set(String key, String value) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try{
            tries ++;
            jedis.set(key,value);
            jedisPool.returnResource(jedis);
        } catch(Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000) {
                jedis = jedisPool.getResource();
                set(key, value);
                jedisPool.returnResource(jedis);
            } else{
                throw new Exception ("Tried 1000 times setting key:"+key+ " and value:"+value+" without success");
            }
        }
    }
//
//    public static synchronized Boolean exists(String key) throws Exception{
//        int tries =0;
//        try{
//            tries++;
//            return jedis.exists(key);
//        }
//
//        catch (Exception e ){
//            if (tries<1000)
//            {
//                getJedis();
//                return exists(key);
//            }
//
//            else {
//                throw new Exception ("Tried 1000 times exists on key:"+key+" without success");
//            }
//        }
//    }

    public static synchronized Set<String> zrange(String key, long start, long end) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            Set<String> results = jedis.zrange(key,start,end);
            jedisPool.returnResource(jedis);
            return results;
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                Set<String> result = zrange(key,start,end);
                jedisPool.returnResource(jedis);
                return result;
            }
            else{
                throw new Exception("Tried 1000 times to get range:"+key+" start:"+start+" end:"+end+" without success");
            }
        }
    }

    public static synchronized Set<String> zrangeByScore (String key, long start, long end) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            Set<String> results = jedis.zrangeByScore(key,start,end);
            jedisPool.returnResource(jedis);
            return results;
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                Set<String> result = zrange(key,start,end);
                jedisPool.returnResource(jedis);
                return result;
            }
            else{
                throw new Exception("Tried 1000 times to get range:"+key+" start:"+start+" end:"+end+" without success");
            }
        }
    }

    public static synchronized void zadd(String key, long score, String value) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            jedis.zadd(key,score,value);
            jedisPool.returnResource(jedis);
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                jedis.zadd(key,score,value);
                jedisPool.returnResource(jedis);
            }
            else{
                throw new Exception("Tried 1000 times to persist :"+value+" without success");
            }
        }
    }

    public static synchronized long zrem(String key, String value) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            long result = jedis.zrem(key,value);
            jedisPool.returnResource(jedis);
            return result;
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                long result = jedis.zrem(key,value);
                jedisPool.returnResource(jedis);
                return result;
            }
            else{
                throw new Exception("Tried 1000 times to remove key: "+key+" value: "+value+" without success");
            }
        }
    }

    public static synchronized void zremrangeByScore(String key, double start, double end) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            jedis.zremrangeByScore(key,start,end);
            jedisPool.returnResource(jedis);
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                jedis.zremrangeByScore(key,start,end);
                jedisPool.returnBrokenResource(jedis);
            }
            else{
                throw new Exception("Tried 1000 times to remove :"+key+" without success");
            }
        }
    }

    public static synchronized Long zcount(String keyName, double min, double max) throws Exception{
        int tries =0;
        Jedis jedis = jedisPool.getResource();
        try {
            tries ++;
            Long result = jedis.zcount(keyName, min, max);
            jedisPool.returnResource(jedis);
            return result;
        }

        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000)
            {
                jedis = jedisPool.getResource();
                Long result = jedis.zcount(keyName,0d,-1);
                jedisPool.returnResource(jedis);
                return result;
            }
            else{
                throw new Exception("Tried 1000 times to zcount :"+keyName+" without success");
            }
        }

    }

    public static synchronized String get(String key) throws Exception{
        int tries = 0;
        Jedis jedis = jedisPool.getResource();
        try{
            tries ++;
            String result = jedis.get(key);
            jedisPool.returnResource(jedis);
            return result;
        }
        catch (Exception e){
            jedisPool.returnBrokenResource(jedis);
            if (tries<1000) {
                jedis = jedisPool.getResource();
                String result = jedis.get(key);
                jedisPool.returnResource(jedis);
                return result;
            }else{
                throw new Exception("Tried 1000 times to get key:"+key+" without success");
            }

        }
    }

    public static void hmset (String mapName, String key, String json){
        Jedis jedis = jedisPool.getResource();
        jedis.hmset(mapName, Map.of(key, json));
        jedisPool.returnResource(jedis);
    }

    public static Optional<String> hmget (String mapName, String key) throws Exception{
        Jedis jedis = jedisPool.getResource();
        List<String> valueList = jedis.hmget(mapName, key);
        jedisPool.returnResource(jedis);
        Optional<String> valueOptional = Optional.empty();
        if (valueList.size()==1 && valueList.get(0)!=null){
            valueOptional = Optional.of(valueList.get(0));
        } else if (valueList.size()>1){
            throw new Exception("Map: "+mapName+" and Key: "+key+" returned "+valueList.size()+" values: should only return one or zero.");
        }
        return valueOptional;
    }


}
