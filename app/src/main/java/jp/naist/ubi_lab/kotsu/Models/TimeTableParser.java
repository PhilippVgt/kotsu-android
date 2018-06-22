package jp.naist.ubi_lab.kotsu.Models;

import java.util.Date;

import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public abstract class TimeTableParser {

    public void setListener(TimeTableListener l) {}
    public void parse(Stop from, Stop to, Date date) {}


    private static TimeTableParser universalParser;

    public static TimeTableParser getParser(TimeTableListener listener) {
        if(universalParser == null) {
            universalParser = new UniversalParser();
        }
        universalParser.setListener(listener);
        return universalParser;
    }

}
