package io.github.jark006.freezeit.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Timer;
import java.util.TimerTask;

import io.github.jark006.freezeit.AppInfoCache;
import io.github.jark006.freezeit.ManagerCmd;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.StaticData;
import io.github.jark006.freezeit.Utils;

public class AppTime extends AppCompatActivity {
    AppTimeAdapter recycleAdapter = new AppTimeAdapter();
    Timer timer;
    int[] newUidTime;
    final int UPDATE_DATA_SET = 1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_time);

        var animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        RecyclerView recyclerView = findViewById(R.id.recyclerviewApp);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recycleAdapter);
        recyclerView.setItemAnimator(animator);
        recyclerView.setHasFixedSize(true);

        Context context = this;
        this.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.apptime_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.help_task) {
                    Utils.layoutDialog(context, R.layout.help_dialog_app_time);
                }
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        findViewById(R.id.container).setBackground(StaticData.getBackgroundDrawable(this));

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                var recvLen = Utils.freezeitTask(ManagerCmd.getUidTime, null);

                // 每个APP时间为3个int32 [0-2]:[uid delta total], 共12字节
                if (recvLen == 0 || recvLen % 12 != 0)
                    return;
                newUidTime = new int[recvLen / 4];
                Utils.Byte2Int(StaticData.response, 0, recvLen, newUidTime, 0);
                handler.sendEmptyMessage(UPDATE_DATA_SET);
            }
        }, 0, 2000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_DATA_SET)
                recycleAdapter.updateDataSet(newUidTime);
        }
    };


    static class AppTimeAdapter extends RecyclerView.Adapter<AppTimeAdapter.MyViewHolder> {
        int[] uidTime = new int[0];
        StringBuilder timeStr = new StringBuilder(32);

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.app_time_layout, parent, false);
            return new MyViewHolder(view);
        }

        @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            position *= 3;
            final int uid = uidTime[position];

            if (holder.uid != uid) {
                holder.uid = uid;
                var info = AppInfoCache.get(uid);
                if (info != null) {
                    holder.app_icon.setImageDrawable(info.icon);
                    holder.app_label.setText(info.label);
                } else {
                    holder.app_label.setText(String.valueOf(uid));
                }
            }

            holder.delta.setText(getTimeStr(uidTime[position + 1])); // deltaTime
            holder.total.setText(getTimeStr(uidTime[position + 2])); // totalTime
        }

        @SuppressLint("DefaultLocale")
        StringBuilder getTimeStr(int time) {
            timeStr.setLength(0);

            if (time <= 0) return timeStr;
            else if (time <= 1000) {
                timeStr.append(time).append("ms");
                return timeStr;
            }

            int ms = time % 1000;
            time /= 1000; // now Unit is second

            if (time >= 3600) {
                timeStr.append(time / 3600).append('h');
                time %= 3600;
            }
            if (time >= 60) {
                timeStr.append(time / 60).append('m');
                time %= 60;
            }

            timeStr.append(time).append('.');

            if (ms >= 100) timeStr.append(ms);
            else if (ms >= 10) timeStr.append('0').append(ms);
            else timeStr.append("00").append(ms);

            timeStr.append('s');
            return timeStr;
        }

        @Override
        public int getItemCount() {
            return uidTime.length / 3;
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateDataSet(@NonNull int[] newUidTime) {
            if (uidTime.length != newUidTime.length) {
                uidTime = newUidTime;
                notifyDataSetChanged();
                return;
            }

            for (int i = 0; i < uidTime.length; i += 3) {
                if (uidTime[i] == newUidTime[i] &&
                        uidTime[i + 1] == newUidTime[i + 1] &&
                        uidTime[i + 2] == newUidTime[i + 2])
                    continue;
                uidTime[i] = newUidTime[i];
                uidTime[i + 1] = newUidTime[i + 1];
                uidTime[i + 2] = newUidTime[i + 2];
                notifyItemChanged(i / 3);
            }
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView app_icon;
            TextView app_label, delta, total;
            int uid = 0;

            public MyViewHolder(View view) {
                super(view);

                app_icon = view.findViewById(R.id.app_icon);
                app_label = view.findViewById(R.id.app_label);
                delta = view.findViewById(R.id.delta);
                total = view.findViewById(R.id.total);
            }
        }
    }
}