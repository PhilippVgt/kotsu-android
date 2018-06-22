package jp.naist.ubi_lab.kotsu.Models;

import java.util.List;

import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public interface StopListener {
    void updated(List<Stop> stops);
}
