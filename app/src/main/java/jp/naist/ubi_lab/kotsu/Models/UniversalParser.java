package jp.naist.ubi_lab.kotsu.Models;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jp.naist.ubi_lab.kotsu.Models.Containers.Departure;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class UniversalParser extends TimeTableParser {

    private static final String TAG = "UniversalParser";

    private TimeTableListener listener;

    private OkHttpClient client = new OkHttpClient();


    @Override
    public void setListener(TimeTableListener l) {
        this.listener = l;
    }

    @Override
    public void parse(Stop from, Stop to, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String url = "https://mphsoft.hadar.uberspace.de/kotsu/departure/" + from.getId() + "/" + to.getId() + "/" + format.format(date);
        Log.i(TAG, "Fetching " + url);

        for(Call call : client.dispatcher().runningCalls()) {
            call.cancel();
        }

        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
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

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                List<Departure> departures = new ArrayList<>();

                for(int i = 0; i < json.length(); i++) {
                    try {
                        JSONObject object = json.getJSONObject(i);

                        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                        cal.setTime(format.parse(object.getString("date")));
                        cal.set(Calendar.HOUR_OF_DAY, object.getInt("hours"));
                        cal.set(Calendar.MINUTE, object.getInt("minutes"));
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        Date time = cal.getTime();

                        String line = object.getString("line");
                        if(!Locale.getDefault().getLanguage().equals("ja")) {
                            line = line.replace("奈", "Kotsu");
                            line = line.replace("関", "KATE");
                        }

                        int platform = object.getInt("platform");
                        int duration = object.getInt("duration");
                        int fare = object.getInt("fare");

                        Stop destination = StopLoader.getInstance().getStop(object.getJSONObject("terminal").getInt("code"));

                        departures.add(new Departure(destination, line, platform, fare, time, duration));
                    } catch(Exception e) {
                        e.printStackTrace();
                        listener.failure();
                        return;
                    }
                }

                if (departures.size() == 0) {
                    listener.failure();
                } else {
                    Collections.sort(departures, new Comparator<Departure>() {
                        @Override
                        public int compare(Departure departure, Departure departure2) {
                            return departure.getTime().compareTo(departure2.getTime());
                        }
                    });
                    listener.success(departures);
                }
            }
        });
    }

}
