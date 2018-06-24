package jp.naist.ubi_lab.kotsu.Models;

import java.util.List;

import jp.naist.ubi_lab.kotsu.Models.Containers.Connection;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public interface StopListener {
    void stops(List<Stop> stops);
    void connections(List<Connection> connections);
}
