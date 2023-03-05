package io.github.jark006.freezeit;

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

public class AppTimeActivity extends AppCompatActivity {
    AppTimeAdapter recycleAdapter;
    int[] uidTime = new int[0];
    Timer timer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_time);

        RecyclerView recyclerView = findViewById(R.id.recyclerviewApp);
        recycleAdapter = new AppTimeAdapter(uidTime);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setAdapter(recycleAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

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
        timer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> Utils.freezeitTask(Utils.getUidTime, null, handler)).start();
            }
        }, 0, 2000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            // 每个APP时间为5个int32, 共20字节  int[0-4]: [uid lastUserTime lastSysTime userTime sysTime]
            if (response == null || response.length == 0 || response.length % 20 != 0)
                return;

            int[] newUidTime = new int[response.length / 4];
            Utils.Byte2Int(response, 0, response.length, newUidTime, 0);

            if (newUidTime.length != uidTime.length) {
                recycleAdapter.update(newUidTime);
                recycleAdapter.notifyItemRangeChanged(0, newUidTime.length / 5);
            } else {
                recycleAdapter.update(newUidTime);
                for (int i = 0; i < newUidTime.length; i += 5) {
                    if (newUidTime[i] == uidTime[i] &&
                            newUidTime[i + 1] == uidTime[i + 1] &&
                            newUidTime[i + 2] == uidTime[i + 2] &&
                            newUidTime[i + 3] == uidTime[i + 3] &&
                            newUidTime[i + 4] == uidTime[i + 4])
                        continue;
                    recycleAdapter.notifyItemChanged(i / 5);
                }
            }
            uidTime = newUidTime;
        }
    };


    public static class AppTimeAdapter extends RecyclerView.Adapter<AppTimeAdapter.MyViewHolder> {
        int[] uidTime;
        StringBuilder timeStr = new StringBuilder(32);

        public AppTimeAdapter(int[] newUidTime) {
            uidTime = newUidTime;
        }

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

            int elementPosition = position * 5;
            int uid = uidTime[elementPosition];

            var info = AppInfoCache.get(uid);
            if (info != null) {
                holder.app_icon.setImageDrawable(info.icon);
                holder.app_label.setText(info.label);
            } else {
                holder.app_label.setText(String.valueOf(uid));
            }
            int lastUserTime = uidTime[elementPosition + 1];
            int lastSysTime = uidTime[elementPosition + 2];
            int userTime = uidTime[elementPosition + 3];
            int sysTime = uidTime[elementPosition + 4];

            holder.userTimeSum.setText(getTimeStr(userTime));
            holder.sysTimeSum.setText(getTimeStr(sysTime));
            holder.userTimeDelta.setText(getTimeStr(userTime - lastUserTime));
            holder.sysTimeDelta.setText(getTimeStr(sysTime - lastSysTime));
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

            if (time >= 10) timeStr.append(time);
            else timeStr.append('0').append(time);

            timeStr.append("s.");

            if (ms >= 100) timeStr.append(ms);
            else if (ms >= 10) timeStr.append('0').append(ms);
            else timeStr.append("00").append(ms);

            timeStr.append("ms");
            return timeStr;
        }

        @Override
        public int getItemCount() {
            return uidTime.length / 5;
        }

        public void update(int[] newUidTime) {
            uidTime = newUidTime;
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView app_icon;
            TextView app_label, userTimeDelta, userTimeSum, sysTimeDelta, sysTimeSum;

            public MyViewHolder(View view) {
                super(view);

                app_icon = view.findViewById(R.id.app_icon);
                app_label = view.findViewById(R.id.app_label);

                userTimeDelta = view.findViewById(R.id.userTimeDelta);
                userTimeSum = view.findViewById(R.id.userTimeSum);
                sysTimeDelta = view.findViewById(R.id.sysTimeDelta);
                sysTimeSum = view.findViewById(R.id.sysTimeSum);

            }
        }
    }
}