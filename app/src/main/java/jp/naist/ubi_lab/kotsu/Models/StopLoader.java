package jp.naist.ubi_lab.kotsu.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
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
import jp.naist.ubi_lab.kotsu.Models.Containers.Connection;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


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
    private List<Connection> allConnections;


    private static List<Stop> defaults = Arrays.asList(
            new Stop(560, "NAIST", "奈良先端科学技術大学院大学"),
            new Stop(558, "Takayama Science Town", "高山サイエンスタウン", false),
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


    private StopLoader(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(!prefs.contains("stops")) {
            allStops = defaults;
            prefs.edit()
                    .putString("stops", new Gson().toJson(defaults))
                    .apply();
        } else {
            allStops = new Gson().fromJson(prefs.getString("stops", ""), new TypeToken<List<Stop>>(){}.getType());
        }

        if(!prefs.contains("connections")) {
            allConnections = new ArrayList<>();
        } else {
            allConnections = new Gson().fromJson(prefs.getString("connections", ""), new TypeToken<List<Connection>>(){}.getType());
        }

        OkHttpClient client = new OkHttpClient();
        Request stopRequest = new Request.Builder()
                .url("https://mphsoft.hadar.uberspace.de/kotsu/stop")
                .build();
        client.newCall(stopRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                JSONArray json;
                try {
                    json = new JSONArray(response.body().string());
                } catch(JSONException e) {
                    e.printStackTrace();
                    return;
                }

                List<Stop> stops = new ArrayList<>();

                for(int i = 0; i < json.length(); i++) {
                    try {
                        JSONObject object = json.getJSONObject(i);

                        Stop stop = new Stop(
                                object.getInt("code"),
                                object.getString("nameEN"),
                                object.getString("nameJP"),
                                object.getBoolean("visible")
                        );

                        stops.add(stop);
                    } catch(Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (stops.size() > 0) {
                    allStops = stops;
                    prefs.edit()
                            .putString("stops", new Gson().toJson(stops))
                            .apply();
                    listener.stops(getAll());
                }
            }
        });


        Request connectionRequest = new Request.Builder()
                .url("https://mphsoft.hadar.uberspace.de/kotsu/connection")
                .build();
        client.newCall(connectionRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONArray json;
                try {
                    json = new JSONArray(response.body().string());
                } catch(JSONException e) {
                    e.printStackTrace();
                    return;
                }

                List<Connection> connections = new ArrayList<>();

                for(int i = 0; i < json.length(); i++) {
                    try {
                        JSONObject object = json.getJSONObject(i);

                        Connection connection = new Connection(
                                StopLoader.getInstance().getStop(object.getJSONObject("from").getInt("code")),
                                StopLoader.getInstance().getStop(object.getJSONObject("to").getInt("code"))
                        );

                        connections.add(connection);
                    } catch(Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                if (connections.size() > 0) {
                    allConnections = connections;
                    prefs.edit()
                            .putString("connections", new Gson().toJson(connections))
                            .apply();
                    listener.connections(connections);
                }
            }
        });
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


    Stop getStop(int id) {
        for(Stop stop : allStops) {
            if(stop.getId() == id) {
                return stop;
            }
        }
        return null;
    }


    public List<Stop> getConnections(Stop from) {
        if(allConnections.size() == 0) {
            List<Stop> connections = new ArrayList<>();
            for(Stop stop : allStops) {
                if(stop.isVisible() && stop.getId() != from.getId()) {
                    connections.add(stop);
                }
            }
            return connections;
        } else {
            List<Stop> connections = new ArrayList<>();
            for(Connection connection : allConnections) {
                if(connection.getFrom().getId() == from.getId()) {
                    if(connection.getTo().isVisible() && connection.getTo().getId() != from.getId()) {
                        connections.add(connection.getTo());
                    }
                }
            }
            return connections;
        }
    }

}
