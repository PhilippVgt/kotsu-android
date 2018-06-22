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

    public boolean isVisible() {
        return visible;
    }


    @Override
    public String toString() {
        return getName();
    }

}
