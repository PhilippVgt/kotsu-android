package jp.naist.ubi_lab.kotsu.Models.Containers;


public class Connection {

    private Stop from, to;

    public Connection(Stop from, Stop to) {
        this.from = from;
        this.to = to;
    }

    public Stop getFrom() {
        return from;
    }

    public Stop getTo() {
        return to;
    }

}
