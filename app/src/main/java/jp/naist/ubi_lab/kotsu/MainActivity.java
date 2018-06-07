package jp.naist.ubi_lab.kotsu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.naist.ubi_lab.kotsu.Models.Containers.Departure;
import jp.naist.ubi_lab.kotsu.Models.Containers.Stop;
import jp.naist.ubi_lab.kotsu.Models.TimeTableListener;
import jp.naist.ubi_lab.kotsu.Models.TimeTableParser;

public class MainActivity extends AppCompatActivity {

    private Date currentDate = new Date();
    private TimeTableListener timeTableListener;

    private Spinner fromSpinner, toSpinner;
    private ImageButton swapButton;

    private SwipeRefreshLayout swipeRefresh;
    private ScrollView scrollView;
    private LinearLayout departuresLayout;

    private LinearLayout nextBusContainer;
    private TextView nextBus;
    private View noConnectionIndicator;

    private Stop from, to;


    private List<Departure> departures = new ArrayList<>();
    private List<View> timeViews = new ArrayList<>();
    private View nextDeparture = null;
    private View nextBusDeparture = null;

    Runnable timeViewUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimeViews();
        }
    };


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Calendar cal = Calendar.getInstance();

            switch (item.getItemId()) {
                case R.id.navigation_tomorrow:
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case R.id.navigation_other:
                    if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                    } else {
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    }
                    cal.add(Calendar.DAY_OF_MONTH, 7);
                    break;
            }
            currentDate = cal.getTime();
            updateTimeTable();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        MenuItem otherItem = navigation.getMenu().findItem(R.id.navigation_other);
        Calendar cal = Calendar.getInstance();
        if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            otherItem.setTitle(R.string.title_weekday);
        } else {
            otherItem.setTitle(R.string.title_weekend);
        }


        ArrayAdapter<Stop> stopAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, Stop.getAll());
        from = stopAdapter.getItem(0);
        fromSpinner = findViewById(R.id.fromSpinner);
        fromSpinner.setAdapter(stopAdapter);
        fromSpinner.setSelection(0);
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(fromSpinner.getSelectedItem() != from) {
                    from = (Stop)fromSpinner.getSelectedItem();
                    updateTimeTable();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        to = stopAdapter.getItem(2);
        toSpinner = findViewById(R.id.toSpinner);
        toSpinner.setAdapter(stopAdapter);
        toSpinner.setSelection(2);
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(toSpinner.getSelectedItem() != to) {
                    to = (Stop) toSpinner.getSelectedItem();
                    updateTimeTable();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        swapButton = findViewById(R.id.swapButton);
        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int toSelection = toSpinner.getSelectedItemPosition();
                toSpinner.setSelection(fromSpinner.getSelectedItemPosition());
                fromSpinner.setSelection(toSelection);
            }
        });

        swipeRefresh = findViewById(R.id.swiperefresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateTimeTable();
            }
        });
        scrollView = findViewById(R.id.scrollview);
        departuresLayout = findViewById(R.id.departures);

        nextBusContainer = findViewById(R.id.next_bus_container);
        nextBusContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(nextBusDeparture != null) {
                    scrollView.smoothScrollTo(0, nextBusDeparture.getTop());
                }
            }
        });
        nextBus = findViewById(R.id.next_bus);

        noConnectionIndicator = findViewById(R.id.no_connection);


        timeTableListener = new TimeTableListener() {
            @Override
            public void success(final List<Departure> depart) {
                departures = depart;

                departuresLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);

                        departuresLayout.removeCallbacks(timeViewUpdater);
                        departuresLayout.removeAllViews();
                        timeViews.clear();
                        nextDeparture = null;

                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        for(Departure departure : departures) {
                            View item = getLayoutInflater().inflate(R.layout.list_item, null);
                            ((TextView)item.findViewById(R.id.destination)).setText(departure.getDestination().getName());
                            ((TextView)item.findViewById(R.id.line)).setText(departure.getLine().isEmpty() ? "-" : departure.getLine());
                            ((TextView)item.findViewById(R.id.platform)).setText(departure.getPlatform() == 0 ? "-" :String.valueOf(departure.getPlatform()));
                            ((TextView)item.findViewById(R.id.duration)).setText(departure.getDuration() == 0 ? "-" :getString(R.string.duration, departure.getDuration()));
                            ((TextView)item.findViewById(R.id.fare)).setText(departure.getFare() == 0 ? "-" : getString(R.string.fare, departure.getFare()));
                            ((TextView)item.findViewById(R.id.time)).setText(dateFormat.format(departure.getTime()));
                            departuresLayout.addView(item);
                            timeViews.add(item);

                            if(nextDeparture == null && getRemainingSeconds(departure) > 0) {
                                nextDeparture = item;
                            }
                        }

                        if(nextDeparture != null) {
                            departuresLayout.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    scrollView.smoothScrollTo(0, nextDeparture.getTop());
                                }
                            }, 500);
                        }

                        noConnectionIndicator.setVisibility(View.GONE);

                        departuresLayout.post(timeViewUpdater);
                    }
                });
            }

            @Override
            public void failure() {
                departuresLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);
                        departuresLayout.removeAllViews();
                        departures.clear();
                        timeViews.clear();
                        noConnectionIndicator.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        updateTimeTable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        departuresLayout.post(timeViewUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        departuresLayout.removeCallbacks(timeViewUpdater);
    }


    private void updateTimeTable() {
        if(from.getId() == to.getId()) {
            swipeRefresh.setRefreshing(false);
            departuresLayout.removeAllViews();
            departures.clear();
            timeViews.clear();
            noConnectionIndicator.setVisibility(View.VISIBLE);
        } else {
            swipeRefresh.setRefreshing(true);
            noConnectionIndicator.setVisibility(View.GONE);
            TimeTableParser.getParser(from, to, timeTableListener).parse(from, to, currentDate);
        }
    }


    private void updateTimeViews() {
        Departure next = null;
        for(int i = 0; i < departures.size(); i++) {
            int remainingSeconds = getRemainingSeconds(departures.get(i));
            int remainingMinutes = getRemainingMinutes(departures.get(i));

            if(next == null && remainingSeconds > 0 && remainingMinutes < 60) {
                next = departures.get(i);
            }

            TextView remainingView = timeViews.get(i).findViewById(R.id.remaining);
            if(remainingSeconds > 0 && remainingSeconds < 60) {
                remainingView.setVisibility(View.VISIBLE);
                remainingView.setText(getString(R.string.remaining_time_sec, remainingSeconds));
            } else if(remainingMinutes > 0 && remainingMinutes < 60) {
                remainingView.setVisibility(View.VISIBLE);
                remainingView.setText(getString(R.string.remaining_time, remainingMinutes));
            } else {
                remainingView.setVisibility(View.GONE);
            }

            if(remainingSeconds < 0) {
                timeViews.get(i).findViewById(R.id.destination).setEnabled(false);
                timeViews.get(i).findViewById(R.id.line).setEnabled(false);
            }
        }

        if(next != null) {
            nextBusDeparture = timeViews.get(departures.indexOf(next));
            if(nextBusContainer.getVisibility() != View.VISIBLE) {
                nextBusContainer.setVisibility(View.VISIBLE);
                nextBusContainer.setTranslationY(500);
                ObjectAnimator animator = ObjectAnimator.ofFloat(nextBusContainer, "translationY", 0);
                animator.setDuration(500);
                animator.setStartDelay(500);
                animator.start();
            }
            nextBus.setText(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(getRemainingTime(next)));
        } else {
            nextBusDeparture = null;
            if(nextBusContainer.getVisibility() != View.GONE) {
                nextBusContainer.setTranslationY(0);
                ObjectAnimator animator = ObjectAnimator.ofFloat(nextBusContainer, "translationY", 500);
                animator.setDuration(500);
                animator.setStartDelay(500);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animator) {
                        nextBusContainer.setVisibility(View.GONE);
                    }
                });
                animator.start();
            }
        }

        departuresLayout.postDelayed(timeViewUpdater, 1000);
    }


    private Date getRemainingTime(Departure departure) {
        Date time = new Date();
        time.setTime(getRemainingSeconds(departure) * 1000);
        return time;
    }

    private int getRemainingMinutes(Departure departure) {
        return getRemainingSeconds(departure) / 60;
    }

    private int getRemainingSeconds(Departure departure) {
        return (int) ((departure.getTime().getTime() - new Date().getTime()) / 1000);
    }

}
