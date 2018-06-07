package jp.naist.ubi_lab.kotsu.Models;

import java.util.List;

import jp.naist.ubi_lab.kotsu.Models.Containers.Departure;


public interface TimeTableListener {
    void success(List<Departure> departures);
    void failure();
}
