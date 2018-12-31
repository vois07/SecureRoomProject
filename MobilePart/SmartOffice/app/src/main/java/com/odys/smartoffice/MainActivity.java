package com.odys.smartoffice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.mainLayout)
    LinearLayout mainLayout;
    @BindView(R.id.dataListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.usernameTextView)
    TextView usernameTextView;
    @BindView(R.id.swipeLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.workButton)
    Button workButton;
    @BindView(R.id.startTimeTextView)
    TextView startTime;
    @BindView(R.id.workTimeChronometer)
    Chronometer workTime;

    private DataListAdapter adapter;
    private List<DataModel> dataList;
    private ServerDataDownloader dataService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        AppConstants.Companion.setOnDataChanged((s, s2) -> {
            refreshView();
            return null;
        });

        AppConstants.Companion.setOnWorkChange((b, b2) -> {
            refreshWork();
            return null;
        });

        PreferencesManager.Companion.setFirstLaunched(this, false);
        setUsername();
        dataService = new ServerDataDownloader(this, mainLayout, this);
        setMeasurementList();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            new Thread(() -> runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false))).start();
            dataService.getData();
        });

        dataService.getData();
        final Handler handler = new Handler();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dataService.getData();
                handler.postDelayed(this, AppConstants.refreshTime);
            }
        }, AppConstants.refreshTime);

        workButton.setOnClickListener(view -> {
            if(AppConstants.Companion.getWork()) {
                dataService.openDoorEndWork();
            } else {
                dataService.openDoorStartWork();
            }
        });

        workTime.setOnChronometerTickListener(chronometer -> {
            long time = SystemClock.elapsedRealtime() - chronometer.getBase();
            int h   = (int)(time /3600000);
            int m = (int)(time - h*3600000)/60000;
            int s= (int)(time - h*3600000- m*60000)/1000 ;
            String t = (h < 10 ? "0"+h: h)+":"+(m < 10 ? "0"+m: m)+":"+ (s < 10 ? "0"+s: s);
            chronometer.setText(t);
        });
        workTime.setBase(SystemClock.elapsedRealtime());
        workTime.setText(R.string.def_time);
    }

    private void refreshWork() {
        if(AppConstants.Companion.getWork()) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String currentTime = sdf.format(new Date());
            startTime.setText(currentTime);
            workButton.setText(getString(R.string.end_work));
            workTime.setBase(SystemClock.elapsedRealtime());
            workTime.setText("00:00:00");
            workTime.start();
        } else {
            workButton.setText(getString(R.string.start_work));
            workTime.stop();
        }
    }

    private void setMeasurementList() {
        dataList = new ArrayList<>();
        adapter = new DataListAdapter(dataList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void refreshView() {
        List<DataModel> newDataList = new ArrayList<>();
        String update = AppConstants.Companion.getDataUpdate();
        String[] nana = update.split("\\$\\$\\$");
        if(nana != new String[]{""}) {
            for(int i=2; i<nana.length; i+=2) {
                if(nana[i].contains("emp")) {
                    newDataList.add(new DataModel(nana[i], nana[i+1]));
                } else if(nana[i].contains("ir")) {
                    String j;
                    if(nana[i+1].equals("0")) {
                        j="dobra";
                    } else {
                        j="zła";
                    }
                    newDataList.add(new DataModel("Jakość powietrza", j));
                }
            }
            dataList.clear();
            dataList.addAll(newDataList);
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setUsername() {
        String s = PreferencesManager.Companion.getUsername(this);
        s = s.substring(0, s.length()-4);
        String[] r = s.split("(?=\\p{Upper})");
        if(r.length > 2) {
            usernameTextView.setText(r[1] + " " + r[2]);
        }
    }

}
