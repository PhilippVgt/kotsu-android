package jp.naist.ubi_lab.kotsu.Models.Containers;


import java.util.Date;

public class Departure {

    private Stop destination;
    private Date time;
    private String line;
    private int platform;
    private int fare;
    private int duration;


    public Departure(Stop destination, String line, int platform, int fare, Date time, int duration) {
        this.destination = destination;
        this.line = line;
        this.platform = platform;
        this.fare = fare;
        this.time = time;
        this.duration = duration;
    }

    public Stop getDestination() {
        return destination;
    }

    public Date getTime() {
        return time;
    }

    public String getLine() {
        return line;
    }

    public int getPlatform() {
        return platform;
    }

    public int getFare() {
        return fare;
    }

    public int getDuration() {
        return duration;
    }
}
