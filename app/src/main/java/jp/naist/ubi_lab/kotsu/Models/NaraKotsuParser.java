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


public class NaraKotsuParser extends TimeTableParser {

    private static final String TAG = "NaraKotsuParser";


    private TimeTableListener listener;
    private WebView webView;

    private Stop queryDestination;

    private Date date;


    public NaraKotsuParser() {
        webView = new WebView(Kotsu.getAppContext());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.evaluateJavascript("setTimeout(function() {"
                            + "if(document.getElementsByClassName('timetable').length > 0) {"
                                + "Android.onPageLoaded(document.getElementsByClassName('timetable')[0].outerHTML);"
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
        this.queryDestination = to;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String url = "https://navi.narakotsu.co.jp/result_timetable/?stop1=" + from.getId() + "&stop2=" + to.getId() + "&date=" + format.format(date);
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
        Element table = doc.getElementsByTag("tbody").first();
        if(table != null) {
            Elements destinations = table.getElementsByClass("destination").first().children();
            Elements platforms = table.getElementsByClass("platform").first().children();
            Elements durations = table.getElementsByClass("required").first().children();
            Elements fares = table.getElementsByClass("fare").first().children();

            Elements minutes = doc.select(".minutes");

            List<Departure> departures = new ArrayList<>();
            for (Element minute : minutes) {
                int hour = Integer.valueOf(minute.parent().parent().getElementsByClass("timezone_link").text());
                int min = Integer.valueOf(minute.text());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                int column = minute.parent().elementSiblingIndex();
                String line = destinations.get(column).getElementsByClass("indicator_no").first().text();
                int platform = Integer.valueOf(platforms.get(column).text().replaceAll("[^\\d.]", ""));
                int duration = Integer.valueOf(durations.get(column).text().replace("分", ""));

                Stop destination = Stop.getStop(destinations.get(column).child(1).text());
                destination = destination != null ? destination : queryDestination;
                destination = destination != null ? destination : new Stop(0, "-", "-");

                int fare = 0;
                if(fares.get(column).getElementsByTag("a").size() > 0) {
                    fare = Integer.valueOf(fares.get(column).getElementsByTag("a").first().text().replace("円", ""));
                } else {
                    fare = Integer.valueOf(fares.get(column).text().replace("円", ""));
                }

                departures.add(new Departure(destination, line, platform, fare, cal.getTime(), duration));
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
        } else {
            listener.failure();
        }
    }

}
