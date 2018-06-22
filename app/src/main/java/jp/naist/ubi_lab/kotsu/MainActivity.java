package jp.naist.ubi_lab.kotsu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import jp.naist.ubi_lab.kotsu.Models.StopListener;
import jp.naist.ubi_lab.kotsu.Models.StopLoader;
import jp.naist.ubi_lab.kotsu.Models.TimeTableListener;
import jp.naist.ubi_lab.kotsu.Models.TimeTableParser;

public class MainActivity extends AppCompatActivity {

    private Date currentDate = new Date();
    private Stop from, to;
    private List<Departure> departures = new ArrayList<>();

    private TimeTableListener timeTableListener;

    private Spinner fromSpinner, toSpinner;

    private SwipeRefreshLayout swipeRefresh;
    private ListView departuresList;
    private MainListAdapter departuresAdapter;

    private LinearLayout nextBusContainer;
    private TextView nextBus;
    private View noConnectionIndicator;

    private int nextBusDeparture = 0;


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
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
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

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        MenuItem otherItem = navigation.getMenu().findItem(R.id.navigation_other);
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            otherItem.setTitle(R.string.title_weekday);
        } else {
            otherItem.setTitle(R.string.title_weekend);
        }


        ArrayAdapter<Stop> stopAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, StopLoader.getInstance().getAll());
        from = stopAdapter.getItem(0);
        fromSpinner = findViewById(R.id.fromSpinner);
        fromSpinner.setAdapter(stopAdapter);
        fromSpinner.setSelection(0);
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (fromSpinner.getSelectedItem() != from) {
                    from = (Stop) fromSpinner.getSelectedItem();
                    updateTimeTable();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        to = stopAdapter.getItem(2);
        toSpinner = findViewById(R.id.toSpinner);
        toSpinner.setAdapter(stopAdapter);
        toSpinner.setSelection(2);
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (toSpinner.getSelectedItem() != to) {
                    to = (Stop) toSpinner.getSelectedItem();
                    updateTimeTable();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        StopLoader.getInstance().setListener(new StopListener() {
            @Override
            public void updated(final List<Stop> stops) {
                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        int fromIndex = 0, toIndex = 2;
                        for (Stop stop : stops) {
                            if (stop.getId() == from.getId()) {
                                fromIndex = stops.indexOf(stop);
                                from = stop;
                            }
                            if (stop.getId() == to.getId()) {
                                toIndex = stops.indexOf(stop);
                                to = stop;
                            }
                        }

                        ArrayAdapter<Stop> stopAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, stops);
                        fromSpinner.setAdapter(stopAdapter);
                        fromSpinner.setSelection(fromIndex);
                        toSpinner.setAdapter(stopAdapter);
                        toSpinner.setSelection(toIndex);
                    }
                });
            }
        });

        ImageButton swapButton = findViewById(R.id.swapButton);
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
        departuresList = findViewById(R.id.departures);
        departuresAdapter = new MainListAdapter(this, departures);
        departuresList.setAdapter(departuresAdapter);
        departuresList.setDividerHeight(0);
        departuresList.setScrollbarFadingEnabled(true);

        nextBusContainer = findViewById(R.id.next_bus_container);
        nextBusContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                smoothScrollToPosition(departuresList, nextBusDeparture);
            }
        });
        nextBus = findViewById(R.id.next_bus);

        noConnectionIndicator = findViewById(R.id.no_connection);


        timeTableListener = new TimeTableListener() {
            @Override
            public void success(final List<Departure> depart) {
                departures = depart;

                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);

                        departuresList.removeCallbacks(timeViewUpdater);
                        departuresAdapter.clear();
                        departuresAdapter.addAll(departures);

                        nextBusDeparture = 0;
                        for (Departure departure : departures) {
                            if (nextBusDeparture == 0 && getRemainingSeconds(departure) > 0) {
                                nextBusDeparture = departures.indexOf(departure);
                            }
                        }

                        departuresList.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                smoothScrollToPosition(departuresList, nextBusDeparture);
                            }
                        }, 500);

                        noConnectionIndicator.setVisibility(View.GONE);
                        swipeRefresh.setVisibility(View.VISIBLE);

                        departuresList.post(timeViewUpdater);
                    }
                });
            }

            @Override
            public void failure() {
                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);
                        departures.clear();
                        departuresAdapter.clear();
                        noConnectionIndicator.setVisibility(View.VISIBLE);
                        swipeRefresh.setVisibility(View.GONE);
                    }
                });
            }
        };

        updateTimeTable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        departuresList.post(timeViewUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        departuresList.removeCallbacks(timeViewUpdater);
    }


    private void updateTimeTable() {
        swipeRefresh.setRefreshing(true);
        noConnectionIndicator.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        TimeTableParser.getParser(from, to, timeTableListener).parse(from, to, currentDate);
    }


    private void updateTimeViews() {
        departuresAdapter.notifyDataSetChanged();

        Departure next = null;
        for (int i = 0; i < departures.size(); i++) {
            int remainingSeconds = getRemainingSeconds(departures.get(i));
            int remainingMinutes = getRemainingMinutes(departures.get(i));

            if (next == null && remainingSeconds > 0 && remainingMinutes < 60) {
                next = departures.get(i);
            }
        }

        if (next != null) {
            nextBusDeparture = departures.indexOf(next);
            if (nextBusContainer.getVisibility() != View.VISIBLE) {
                nextBusContainer.setVisibility(View.VISIBLE);
                nextBusContainer.setTranslationY(500);
                ObjectAnimator animator = ObjectAnimator.ofFloat(nextBusContainer, "translationY", 0);
                animator.setDuration(500);
                animator.setStartDelay(500);
                animator.start();
            }
            nextBus.setText(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(getRemainingTime(next)));
        } else {
            nextBusDeparture = 0;
            if (nextBusContainer.getVisibility() != View.GONE) {
                nextBusContainer.setTranslationY(0);
                ObjectAnimator animator = ObjectAnimator.ofFloat(nextBusContainer, "translationY", 500);
                animator.setDuration(500);
                animator.setStartDelay(500);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        nextBusContainer.setVisibility(View.GONE);
                    }
                });
                animator.start();
            }
        }

        departuresList.postDelayed(timeViewUpdater, 1000);
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


    public static void smoothScrollToPosition(final AbsListView view, final int position) {
        View child = getChildAtPosition(view, position);
        if ((child != null) && ((child.getTop() == 0) || ((child.getTop() > 0) && !view.canScrollVertically(1)))) {
            return;
        }

        view.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    view.setOnScrollListener(null);

                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            view.setSelection(position);
                        }
                    });
                }
            }

            @Override
            public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                                 final int totalItemCount) { }
        });

        view.post(new Runnable() {
            @Override
            public void run() {
                view.smoothScrollToPositionFromTop(position, 0);
            }
        });
    }

    public static View getChildAtPosition(final AdapterView view, final int position) {
        final int index = position - view.getFirstVisiblePosition();
        if ((index >= 0) && (index < view.getChildCount())) {
            return view.getChildAt(index);
        } else {
            return null;
        }
    }


    private class MainListAdapter extends ArrayAdapter<Departure> {

        private class ViewHolder {
            TextView destination, remaining, time, line, platform, duration, fare;
        }


        public MainListAdapter(Context context, List<Departure> departures) {
            super(context, R.layout.list_item, departures);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Departure departure = getItem(position);

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);

                viewHolder.destination = convertView.findViewById(R.id.destination);
                viewHolder.remaining = convertView.findViewById(R.id.remaining);
                viewHolder.time = convertView.findViewById(R.id.time);
                viewHolder.line = convertView.findViewById(R.id.line);
                viewHolder.platform = convertView.findViewById(R.id.platform);
                viewHolder.duration = convertView.findViewById(R.id.duration);
                viewHolder.fare = convertView.findViewById(R.id.fare);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            viewHolder.time.setText(dateFormat.format(departure.getTime()));
            viewHolder.destination.setText(departure.getDestination().getName());
            viewHolder.line.setText(departure.getLine().isEmpty() ? "-" : departure.getLine());
            viewHolder.platform.setText(departure.getPlatform() == 0 ? "-" : String.valueOf(departure.getPlatform()));
            viewHolder.duration.setText(departure.getDuration() == 0 ? "-" : getString(R.string.duration, departure.getDuration()));
            viewHolder.fare.setText(departure.getFare() == 0 ? "-" : getString(R.string.fare, departure.getFare()));

            updateRemaining(departure, viewHolder);

            return convertView;
        }

        private void updateRemaining(Departure departure, ViewHolder viewHolder) {
            int remainingSeconds = getRemainingSeconds(departure);
            int remainingMinutes = getRemainingMinutes(departure);

            if (remainingSeconds > 0 && remainingSeconds < 60) {
                viewHolder.remaining.setVisibility(View.VISIBLE);
                viewHolder.remaining.setText(getString(R.string.remaining_time_sec, remainingSeconds));
            } else if (remainingMinutes > 0 && remainingMinutes < 60) {
                viewHolder.remaining.setVisibility(View.VISIBLE);
                viewHolder.remaining.setText(getString(R.string.remaining_time, remainingMinutes));
            } else {
                viewHolder.remaining.setVisibility(View.GONE);
            }

            viewHolder.destination.setEnabled(remainingSeconds >= 0);
            viewHolder.line.setEnabled(remainingSeconds >= 0);
        }

    }

}
