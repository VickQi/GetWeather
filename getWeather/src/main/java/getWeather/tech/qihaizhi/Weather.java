package getWeather.tech.qihaizhi;

import java.io.Serializable;
import java.util.List;

public class Weather implements Serializable{
    private String status;//返回状态
    private String count;//返回总数
    private String info;//返回德状态信息
    private String infoCode;//返回的状态码
    private List<Live> lives;//实时天气
    private List<Forecast> forecasts;//预报

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfoCode() {
        return infoCode;
    }

    public void setInfoCode(String infoCode) {
        this.infoCode = infoCode;
    }

    public List<Live> getLives() {
        return lives;
    }

    public void setLives(List<Live> lives) {
        this.lives = lives;
    }

    public List<Forecast> getForecasts() {
        return forecasts;
    }

    public void setForecasts(List<Forecast> forecasts) {
        this.forecasts = forecasts;
    }
}
