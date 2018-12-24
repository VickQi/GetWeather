import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] agrs){
        Long tid = System.nanoTime();
        logger.info("{}: 开始获取天气信息",tid);
        //高德天气
        String appKey = "943ea8b9cddf28eff38c1d62d1255377";
        String base_url = "https://restapi.amap.com/v3/weather/weatherInfo?key="+appKey+"&city=110000&extensions=";
        String cur_url = base_url+"base";//实时天气
        String fore_url = base_url+"all";//预报天气

        //获取实时天气
        Weather cur_weather = getWeather(cur_url,tid);
        Weather fore_weather = getWeather(fore_url,tid);
        Weather weather = cur_weather;
        weather.setForecasts(fore_weather.getForecasts());
        Live live = weather.getLives().get(0);
        String weatherReport = "现在天气：" + live.getWeather() + ", " +
                "温度：" + live.getTemperature() + "摄氏度, "+
                "湿度：" + live.getHumidity()+"%,"+
                live.getWinddirection()+"风"+","+
                "风力"+live.getWindpower()+"级, ";
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
                            weatherReport+="今日最高气温："+tempDay+"摄氏度, 最低气温："+tempNight+"摄氏度，";
                        }else{
                            weatherReport+="今日最高气温："+tempNight+"摄氏度, 最低气温："+tempDay+"摄氏度，";
                        }
                    }else if(date.equals(tomarrow)){
                        weatherReport+="明日白天："+cast.getDayweather()+", 明日夜间："+cast.getNightweather()+", ";
                        Integer tempDay = Integer.valueOf(cast.getDaytemp());
                        Integer tempNight = Integer.valueOf(cast.getNighttemp());
                        if(tempDay>tempNight){
                            weatherReport += "明日最高气温："+tempDay+"摄氏度, 最低气温："+tempNight+"摄氏度，";
                        }else{
                            weatherReport += "明日最高气温："+tempNight+"摄氏度, 最低气温："+tempDay+"摄氏度。";
                        }
                    }
                }
            }
            weatherReport += "（更新时间：" + live.getReporttime()+ "）";
        }catch (ParseException e){
            logger.error(tid+": 日期["+live.getReporttime()+"] 解析失败，不符合yyyy-MM-dd格式！");
        }
        logger.info("{}:weatherReport={}",tid,weatherReport);
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
}

