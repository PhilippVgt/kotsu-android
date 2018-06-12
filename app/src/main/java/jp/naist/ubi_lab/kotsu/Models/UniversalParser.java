package jp.naist.ubi_lab.kotsu.Models;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import jp.naist.ubi_lab.kotsu.Models.Containers.Departure;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;


public class UniversalParser extends TimeTableParser {

    private static final String TAG = "NaraKotsuParser";

    private TimeTableListener listener;


    @Override
    public void setListener(TimeTableListener l) {
        this.listener = l;
    }

    @Override
    public void parse(Stop from, Stop to, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String url = "https://mphsoft.hadar.uberspace.de/kotsu/departure/" + from.getId() + "/" + to.getId() + "/" + format.format(date);
        Log.i(TAG, "Fetching " + url);

        RetrieveTask task = new RetrieveTask();
        task.execute(url);
    }


    protected class RetrieveTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {

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
                listener.failure();
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


            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            List<Departure> departures = new ArrayList<>();

            for(int i = 0; i < response.length(); i++) {
                try {
                    JSONObject json = response.getJSONObject(i);

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(format.parse(json.getString("date")));
                    cal.set(Calendar.HOUR_OF_DAY, json.getInt("hours"));
                    cal.set(Calendar.MINUTE, json.getInt("minutes"));
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date time = cal.getTime();

                    String line = json.getString("line");
                    if(!Locale.getDefault().getLanguage().equals("ja")) {
                        line = line.replace("奈", "Kotsu");
                        line = line.replace("関", "KATE");
                    }

                    int platform = json.getInt("platform");
                    int duration = json.getInt("duration");
                    int fare = json.getInt("fare");

                    Stop destination = Stop.getStop(json.getJSONObject("terminal").getInt("code"));

                    departures.add(new Departure(destination, line, platform, fare, time, duration));
                } catch(Exception e) {
                    e.printStackTrace();
                    listener.failure();
                    return null;
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

            return null;
        }
    }

}
