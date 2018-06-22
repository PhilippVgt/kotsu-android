package jp.naist.ubi_lab.kotsu.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.naist.ubi_lab.kotsu.Kotsu;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public class StopLoader {

    private static final String TAG = "StopLoader";


    private static StopLoader instance;
    public static StopLoader getInstance() {
        if(instance == null) {
            instance = new StopLoader(Kotsu.getAppContext());
        }
        return instance;
    }


    private StopListener listener;
    private SharedPreferences prefs;

    private List<Stop> allStops;


    public static List<Stop> defaults = Arrays.asList(
            new Stop(560, "NAIST", "奈良先端科学技術大学院大学"),
            new Stop(558, "Takayama Science Town", "高山サイエンスタウン"),
            new Stop(2610, "Gakken-Kita-Ikoma Station", "学研北生駒駅"),
            new Stop(-1, "Gakuemmae Station", "学園前駅"),
            new Stop(37, "Kintetsu Nara Station", "近鉄奈良駅"),
            new Stop(-6, "JR Nara Station", "ＪＲ奈良駅"),
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


    public StopLoader(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(!prefs.contains("stops")) {
            allStops = defaults;
            prefs.edit()
                    .putString("stops", new Gson().toJson(defaults))
                    .apply();
        } else {
            allStops = new Gson().fromJson(prefs.getString("stops", ""), new TypeToken<List<Stop>>(){}.getType());
        }

        new RetrieveTask().execute("https://mphsoft.hadar.uberspace.de/kotsu/stop");
    }


    public void setListener(StopListener listener) {
        this.listener = listener;
    }


    public List<Stop> getAll() {
        List<Stop> returnValue = new ArrayList<>();
        for (Stop stop : allStops) {
            if (stop.isVisible()) {
                returnValue.add(stop);
            }
        }
        return returnValue;
    }


    public Stop getStop(int id) {
        for(Stop stop : allStops) {
            if(stop.getId() == id) {
                return stop;
            }
        }
        return null;
    }

    public Stop getStop(String jp) {
        for(Stop stop : allStops) {
            if(jp.contains(stop.getNameJP())) {
                return stop;
            }
        }
        return null;
    }


    protected class RetrieveTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.i(TAG, "Fetching " + params[0]);

            URLConnection urlConn;
            BufferedReader bufferedReader = null;
            JSONArray response;

            try {
                URL url = new URL(params[0]);
                urlConn = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }

                response = new JSONArray(stringBuffer.toString());

            } catch(Exception ex) {
                Log.e(TAG, "JSON expection", ex);
                return null;
            } finally {
                if(bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


            List<Stop> stops = new ArrayList<>();

            for(int i = 0; i < response.length(); i++) {
                try {
                    JSONObject json = response.getJSONObject(i);

                    Stop stop = new Stop(
                            json.getInt("code"),
                            json.getString("nameEN"),
                            json.getString("nameJP"),
                            json.getBoolean("visible")
                    );

                    stops.add(stop);
                } catch(Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            if (stops.size() > 0) {
                allStops = stops;
                prefs.edit()
                        .putString("stops", new Gson().toJson(stops))
                        .apply();
                listener.updated(getAll());
            }

            return null;
        }
    }

}
