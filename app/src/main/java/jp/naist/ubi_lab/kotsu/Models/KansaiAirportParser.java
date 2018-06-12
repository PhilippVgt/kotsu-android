package jp.naist.ubi_lab.kotsu.Models;


import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.naist.ubi_lab.kotsu.Kotsu;
import jp.naist.ubi_lab.kotsu.Models.Containers.Departure;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;

public class KansaiAirportParser extends TimeTableParser {

    private static final String TAG = "KansaiAirportParser";


    private TimeTableListener listener;
    private WebView webView;

    private Stop queryLocation, queryDestination;

    private Date date;


    public KansaiAirportParser() {
        webView = new WebView(Kotsu.getAppContext());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.evaluateJavascript("setTimeout(function() {"
                        + "if(document.getElementsByClassName('timetable').length > 0) {"
                        + "Android.onPageLoaded(document.body.innerHTML);"
                        + "} else {"
                        + "Android.onPageFailed();"
                        + "}"
                        + "}, 1000);", null);
            }
        });
        webView.addJavascriptInterface(this, "Android");
    }


    @Override
    public void setListener(TimeTableListener l) {
        this.listener = l;
    }

    @Override
    public void parse(Stop from, Stop to, Date date) {
        webView.stopLoading();

        this.date = date;
        this.queryLocation = from;
        this.queryDestination = to;
        String url = "http://www.kate.co.jp/timetable/detail/GK";
        Log.i(TAG, "Fetching " + url);

        webView.loadUrl(url);
    }

    @JavascriptInterface
    public final void onPageLoaded(String html) {
        html = html.replace("\\u003C", "<");
        html = html.replace("\\t", "");
        html = html.replace("\\n", "");
        html = html.replace("\\\"", "\"");

        Document doc = Jsoup.parseBodyFragment(html);
        extract(doc);
    }

    @JavascriptInterface
    public final void onPageFailed() {
        listener.failure();
    }

    private void extract(Document doc) {
        if(doc == null || doc.select(".timetable").size() < 2) {
            listener.failure();
            return;
        }

        Element timeTable;
        if (queryLocation.getId() == 100001 || queryLocation.getId() == 100002) {
            timeTable = doc.select(".timetable").get(1);
        } else {
            timeTable = doc.select(".timetable").get(0);
        }

        if(timeTable == null) {
            listener.failure();
            return;
        }

        Elements fromStops = timeTable.select(".dep_box .name");
        int fromColumn = -1;
        for(Element fromStop : fromStops) {
            if(fromStop.text().contains(queryLocation.getNameJP())) {
                fromColumn = fromStops.indexOf(fromStop);
                break;
            }
        }

        Elements toStops = timeTable.select(".arr_box .name");
        int toColumn = -1;
        for(Element toStop : toStops) {
            if(toStop.text().contains(queryDestination.getNameJP())) {
                toColumn = fromStops.size() + toStops.indexOf(toStop);
                break;
            }
        }

        if(fromColumn < 0 || toColumn < 0) {
            listener.failure();
            return;
        }

        int fare = Integer.valueOf(doc.select(".fare_area").first().text().replace("円", "").replace(",", ""));


        List<Departure> departures = new ArrayList<>();

        Elements rows = timeTable.select(".time");
        for(int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);

            String company = row.getElementsByClass("company").get(0).text();
            if(!Locale.getDefault().getLanguage().equals("ja")) {
                company = company.replace("奈", "Kotsu");
                company = company.replace("関", "KATE");
            }

            Date departureTime, arrivalTime;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            try {
                String[] time = row.children().get(fromColumn + 1).text().split(":");
                if(time.length < 2) continue;
                cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
                cal.set(Calendar.MINUTE, Integer.valueOf(time[1]));
                departureTime = cal.getTime();
                time = row.children().get(toColumn + 1).text().split(":");
                if(time.length < 2) continue;
                cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
                cal.set(Calendar.MINUTE, Integer.valueOf(time[1]));
                arrivalTime = cal.getTime();
            } catch(Exception e) {
                e.printStackTrace();
                listener.failure();
                return;
            }
            int duration = (int)((arrivalTime.getTime() - departureTime.getTime()) / (1000*60));

            departures.add(new Departure(queryDestination, company, 0, fare, departureTime, duration));
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


}
