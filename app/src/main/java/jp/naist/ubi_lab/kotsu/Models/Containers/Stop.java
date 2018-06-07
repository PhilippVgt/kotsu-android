package jp.naist.ubi_lab.kotsu.Models.Containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class Stop {

    private int id;
    private String nameEN;
    private String nameJP;

    private boolean visible = true;


    public Stop(int id, String nameEN, String nameJP) {
        this.id = id;
        this.nameEN = nameEN;
        this.nameJP = nameJP;
    }

    public Stop(int id, String nameEN, String nameJP, boolean visible) {
        this(id, nameEN, nameJP);
        this.visible = visible;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        if(Locale.getDefault().getLanguage().equals("ja") || Locale.getDefault().getLanguage().equals("zh")) {
            return getNameJP();
        } else {
            return getNameEN();
        }
    }

    public String getNameEN() {
        return nameEN;
    }

    public String getNameJP() {
        return nameJP;
    }


    @Override
    public String toString() {
        return getName();
    }



    public static List<Stop> allStops = Arrays.asList(
            new Stop(560, "NAIST", "奈良先端科学技術大学院大学"),
            new Stop(558, "Takayama Science Town", "高山サイエンスタウン"),
            new Stop(2610, "Gakken-Kita-Ikoma Station", "学研北生駒駅"),
            new Stop(-1, "Gakuemmae Station", "学園前駅"),
            new Stop(631, "Chiku Center", "地区センター"),
            new Stop(632, "Shiki-no-Mori Kōen", "四季の森公園"),
            new Stop(-14, "Gakken-Nara-Tomigaoka Station", "学研奈良登美ヶ丘駅"),
            new Stop(-5601, "Takanohara Station", "高の原駅"),
            new Stop(606, "Keihanna Plaza", "けいはんなプラザ"),

            new Stop(100001, "Kansai Airport Terminal 1", "関西空港第1ターミナル"),
            new Stop(100002, "Kansai Airport Terminal 2", "関西空港第2ターミナル"),

            new Stop(604, "Gyoen Station", "祝園駅", false),
            new Stop(625, "Kano-no-Kita 2-Chome", "鹿ノ台北二丁目", false)
    );

    public static List<Stop> getAll() {
        List<Stop> stops = new ArrayList<>();
        for(Stop stop : allStops) {
            if(stop.visible) {
                stops.add(stop);
            }
        }
        return stops;
    }

    public static Stop getStop(int id) {
        for(Stop stop : allStops) {
            if(stop.getId() == id) {
                return stop;
            }
        }
        return null;
    }

    public static Stop getStop(String jp) {
        for(Stop stop : allStops) {
            if(jp.contains(stop.getNameJP())) {
                return stop;
            }
        }
        return null;
    }

}
