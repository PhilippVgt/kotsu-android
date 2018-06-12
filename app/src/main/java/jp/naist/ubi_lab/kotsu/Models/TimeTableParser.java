package jp.naist.ubi_lab.kotsu.Models;

import java.util.Date;

import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public abstract class TimeTableParser {

    public void setListener(TimeTableListener l) {}
    public void parse(Stop from, Stop to, Date date) {}


    private static TimeTableParser naraKotsuParser;
    private static TimeTableParser kansaiAirportParser;
    private static TimeTableParser universalParser;

    public static TimeTableParser getParser(Stop from, Stop to, TimeTableListener listener) {
        /*if(from.getId() == 100001 || from.getId() == 100002 || to.getId() == 100001 || to.getId() == 100002) {
            if(kansaiAirportParser == null) {
                kansaiAirportParser = new KansaiAirportParser();
            }
            kansaiAirportParser.setListener(listener);
            return kansaiAirportParser;
        } else {
            if(naraKotsuParser == null) {
                naraKotsuParser = new NaraKotsuParser();
            }
            naraKotsuParser.setListener(listener);
            return naraKotsuParser;
        }*/

        if(universalParser == null) {
            universalParser = new UniversalParser();
        }
        universalParser.setListener(listener);
        return universalParser;
    }

}
