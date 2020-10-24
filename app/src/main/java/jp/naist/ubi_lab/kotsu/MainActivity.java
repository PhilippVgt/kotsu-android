package jp.naist.ubi_lab.kotsu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jp.naist.ubi_lab.kotsu.Models.Containers.Connection;
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
            Calendar cal = Calendar.getInstance(TimeZone.getDefault());

            switch (item.getItemId()) {
                case R.id.navigation_tomorrow:
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case R.id.navigation_other:
                    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                        if(cal.getTime().getTime() < new Date().getTime()) {
                            cal.add(Calendar.DAY_OF_MONTH, 7);
                        }
                    } else {
                        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        cal.add(Calendar.DAY_OF_MONTH, 7);
                    }
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
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            otherItem.setTitle(R.string.title_weekday);
        } else {
            otherItem.setTitle(R.string.title_weekend);
        }


        ArrayAdapter<Stop> fromAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, StopLoader.getInstance().getAll());
        from = fromAdapter.getItem(0);
        fromSpinner = findViewById(R.id.fromSpinner);
        fromSpinner.setAdapter(fromAdapter);
        fromSpinner.setSelection(0);
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (fromSpinner.getSelectedItem() != from) {
                    updateConnections();
                    updateTimeTable();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        final ArrayAdapter<Stop> toAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, StopLoader.getInstance().getAll());
        to = toAdapter.getItem(2);
        toSpinner = findViewById(R.id.toSpinner);
        toSpinner.setAdapter(toAdapter);
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (toSpinner.getSelectedItem() != to) {
                    to = (Stop) toSpinner.getSelectedItem();
                    updateTimeTable();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        updateConnections();
        toSpinner.setSelection(0);

        StopLoader.getInstance().setListener(new StopListener() {
            @Override
            public void stops(final List<Stop> stops) {
                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        int fromIndex = 0;
                        for (Stop stop : stops) {
                            if (stop.getId() == from.getId()) {
                                fromIndex = stops.indexOf(stop);
                                from = stop;
                            }
                        }

                        ArrayAdapter<Stop> fromAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, stops);
                        fromSpinner.setAdapter(fromAdapter);
                        fromSpinner.setSelection(fromIndex);
                    }
                });
            }
            @Override
            public void connections(final List<Connection> connections) {
                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        updateConnections();
                    }
                });
            }
        });

        ImageButton swapButton = findViewById(R.id.swapButton);
        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Stop fromStop = (Stop) fromSpinner.getSelectedItem();
                Stop toStop = (Stop) toSpinner.getSelectedItem();

                List<Stop> stops = StopLoader.getInstance().getAll();

                int fromIndex = 0;
                for(Stop stop : stops) {
                    if(stop.getId() == toStop.getId()) {
                        fromIndex = stops.indexOf(stop);
                    }
                }
                fromSpinner.setSelection(fromIndex);
                updateConnections();

                int toIndex = 0;
                for(int i = 0; i < toSpinner.getAdapter().getCount(); i++) {
                    Stop toCandidate = (Stop) toSpinner.getAdapter().getItem(i);
                    if(toCandidate.getId() == fromStop.getId()) {
                        toIndex = i;
                    }
                }
                toSpinner.setSelection(toIndex);
            }
        });

        ImageButton moreButton = findViewById(R.id.moreButton);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId() == R.id.navigation_privacy) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/66c49a7cbbb174eae9de7c01cdfb979a"));
                            startActivity(browserIntent);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.more, popup.getMenu());
                popup.show();
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
                departuresList.removeCallbacks(timeViewUpdater);

                departures = depart;

                departuresList.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(false);

                        departuresAdapter.clear();
                        departuresAdapter.addAll(departures);

                        nextBusDeparture = -1;
                        for (Departure departure : departures) {
                            if (nextBusDeparture < 0 && getRemainingSeconds(departure) > 0) {
                                nextBusDeparture = departures.indexOf(departure);
                            }
                        }

                        departuresList.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                smoothScrollToPosition(departuresList, Math.max(0, nextBusDeparture));
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
                departuresList.removeCallbacks(timeViewUpdater);

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


    private void updateConnections() {
        from = (Stop) fromSpinner.getSelectedItem();
        to = (Stop) toSpinner.getSelectedItem();

        List<Stop> connections = StopLoader.getInstance().getConnections(from);
        if(connections == null) {
            connections = StopLoader.getInstance().getAll();
        }

        int toIndex = 0;
        for (Stop stop : connections) {
            if (stop.getId() == to.getId()) {
                toIndex = connections.indexOf(stop);
                to = stop;
            }
        }

        ArrayAdapter<Stop> toAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, connections);
        toSpinner.setAdapter(toAdapter);
        toSpinner.setSelection(toIndex);
    }


    private void updateTimeTable() {
        swipeRefresh.setRefreshing(true);
        noConnectionIndicator.setVisibility(View.GONE);
        swipeRefresh.setVisibility(View.VISIBLE);
        TimeTableParser.getParser(timeTableListener).parse(from, to, currentDate);
    }


    private void updateTimeViews() {
        departuresAdapter.notifyDataSetChanged();

        Departure next = null;
        int nextIndex = -1;
        for (int i = 0; i < departures.size(); i++) {
            int remainingSeconds = getRemainingSeconds(departures.get(i));
            int remainingMinutes = getRemainingMinutes(departures.get(i));

            if (next == null && remainingSeconds > 0 && remainingMinutes < 60) {
                next = departures.get(i);
            }
            if (nextIndex < 0 && remainingSeconds > 0) {
                nextIndex = i;
            }
        }
        nextBusDeparture = Math.max(0, nextIndex);

        if (next != null) {
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


        MainListAdapter(Context context, List<Departure> departures) {
            super(context, R.layout.list_item, departures);
        }


        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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

            Departure departure = getItem(position);
            if(departure != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getDefault());
                viewHolder.time.setText(dateFormat.format(departure.getTime()));
                viewHolder.destination.setText(departure.getDestination().getName());
                viewHolder.line.setText(departure.getLine().isEmpty() ? "-" : departure.getLine());
                viewHolder.platform.setText(departure.getPlatform() == 0 ? "-" : String.valueOf(departure.getPlatform()));
                viewHolder.duration.setText(departure.getDuration() == 0 ? "-" : getString(R.string.duration, departure.getDuration()));
                viewHolder.fare.setText(departure.getFare() == 0 ? "-" : getString(R.string.fare, departure.getFare()));

                updateRemaining(departure, viewHolder);
            }

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
