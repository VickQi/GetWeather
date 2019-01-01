package getWeather.tech.qihaizhi;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {
    private final static Logger logger = LogManager.getLogger(Main.class);
    private static JedisPool jedisPool =  null;
    static{
        try{
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(1024);
            config.setMaxIdle(200);
            config.setMaxWaitMillis(10000);//10s
            config.setTestOnBorrow(true);
            jedisPool = new JedisPool(config,"127.0.0.1",6379,10000);
        }catch (Exception e){
            logger.error("初始化Jedis失败",e);
        }
    }
    public static void main(String[] agrs){
        Long tid = System.nanoTime();
        logger.info("{}: 开始获取天气信息",tid);
        //高德天气
        String appKey = "943ea8b9cddf28eff38c1d62d1255377";
        String base_url = "https://restapi.amap.com/v3/weather/weatherInfo?key="+appKey+"&city=110000&extensions=";
        String cur_url = base_url+"base";//实时天气
        String fore_url = base_url+"all";//预报天气

        //获取实时天气
        Weather weather = getWeather(cur_url,tid);
        Weather fore_weather = getWeather(fore_url,tid);
        weather.setForecasts(fore_weather.getForecasts());
        Live live = weather.getLives().get(0);
        StringBuilder weatherReport = new StringBuilder();
        weatherReport.append("现在天气：");
        weatherReport.append(live.getWeather());
        weatherReport.append(", ");
        weatherReport.append("温度：");
        weatherReport.append(live.getTemperature());
        weatherReport.append("摄氏度, ");
        weatherReport.append("湿度：" );
        weatherReport.append(live.getHumidity());
        weatherReport.append("%,");
        weatherReport.append(live.getWinddirection());
        weatherReport.append("风");
        weatherReport.append(",");
        weatherReport.append("风力");
        weatherReport.append(live.getWindpower());
        weatherReport.append("级, ");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try{
            Date today = format.parse(live.getReporttime());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.add(Calendar.DATE,1);
            Date tomarrow = calendar.getTime();
            List<Forecast> forecastList = weather.getForecasts();
            Forecast forecast = forecastList.get(0);
            List<Cast> castList = forecast.getCasts();
            if(castList!=null){
                for(Cast cast:castList){
                    Date date = format.parse(cast.getDate());
                    if(date.equals(today)){
                        Integer tempDay = Integer.valueOf(cast.getDaytemp());
                        Integer tempNight = Integer.valueOf(cast.getNighttemp());
                        if(tempDay>tempNight){
                            weatherReport.append("今日最高气温：");
                            weatherReport.append(tempDay);
                            weatherReport.append("摄氏度, 最低气温：");
                            weatherReport.append(tempNight);
                            weatherReport.append("摄氏度，");
                        }else{
                            weatherReport.append("今日最高气温：");
                            weatherReport.append(tempNight);
                            weatherReport.append("摄氏度, 最低气温：");
                            weatherReport.append(tempDay);
                            weatherReport.append("摄氏度，");
                        }
                    }else if(date.equals(tomarrow)){
                        weatherReport.append("明日白天：");
                        weatherReport.append(cast.getDayweather());
                        weatherReport.append(", 明日夜间：");
                        weatherReport.append(cast.getNightweather());
                        weatherReport.append(", ");
                        Integer tempDay = Integer.valueOf(cast.getDaytemp());
                        Integer tempNight = Integer.valueOf(cast.getNighttemp());
                        if(tempDay>tempNight){
                            weatherReport.append("明日最高气温：");
                            weatherReport.append(tempDay);
                            weatherReport.append("摄氏度, 最低气温：");
                            weatherReport.append(tempNight);
                            weatherReport.append("摄氏度，");
                        }else{
                            weatherReport.append("明日最高气温：");
                            weatherReport.append(tempNight);
                            weatherReport.append("摄氏度, 最低气温：");
                            weatherReport.append(tempDay);
                            weatherReport.append("摄氏度。");
                        }
                    }
                }
            }
            weatherReport.append("（更新时间：");
            weatherReport.append(live.getReporttime());
            weatherReport.append("）");
            Jedis jedis = getJedis();
            if(jedis != null){
                jedis.set("weather",weatherReport.toString());
                logger.info("{} : 缓存设置成功！weather={}",tid,weatherReport.toString());
            }else{
                logger.error("{}: 设置缓存失败！",tid);
                //TODO 设置数据库
            }
            if(jedisPool!=null){
                jedisPool.close();
            }

        }catch (ParseException e){
            logger.error(tid+": 日期["+live.getReporttime()+"] 解析失败，不符合yyyy-MM-dd格式！");
        }
        logger.info("{}: weatherReport = {}",tid,weatherReport.toString());
    }

    private static Weather getWeather(String cur_url,Long tid) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(cur_url);
        boolean stopLoop = false;
        int maxCnt = 5;
        int round = 0;
        CloseableHttpResponse httpResponse = null;
        Weather weather = null;
        while(!stopLoop&&round<=maxCnt){
            if(round>0){
                logger.info("{}: 开始第{}次重试",tid,round);
            }
            try{
                httpResponse = httpClient.execute(httpGet);
                if(httpResponse.getStatusLine()!=null){
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if(statusCode==200){
                        HttpEntity httpEntity = httpResponse.getEntity();
                        String content = EntityUtils.toString(httpEntity,"UTF-8");
                        Gson gson = new Gson();
                        weather = gson.fromJson(content,Weather.class);
                        stopLoop = true;
                    }else{
                        throw new Exception("HTTP ERROR, code = "+ statusCode);
                    }
                }else{
                    throw new Exception("statusLine实体为空！");
                }
                break;
            }catch (Exception e){
                logger.error(tid+": 获取响应失败！",e);
                round++;
            }finally {
                if(httpResponse!=null){
                    try{
                        httpResponse.close();
                    }catch (IOException e){
                        logger.error(tid+ ": 关闭httpResponse异常！");
                    }
                }
            }
        }
        try{
            httpClient.close();
        }catch (IOException e){
            logger.error(tid+": 关闭httpClient异常！");
        }
        return weather;
    }

    private synchronized static Jedis getJedis(){
        try{
            if(jedisPool!=null){
                return jedisPool.getResource();
            }else{
                return null;
            }
        }catch (Exception e){
            logger.error("获取jedis异常",e);
            return null;
        }
    }
}

