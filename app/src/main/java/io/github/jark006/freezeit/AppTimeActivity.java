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

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class AppTimeActivity extends AppCompatActivity {
    AppTimeAdapter recycleAdapter;
    String[] lines = new String[]{};
    Timer timer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_time);

        RecyclerView recyclerView = findViewById(R.id.recyclerviewApp);
        recycleAdapter = new AppTimeAdapter(lines);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setAdapter(recycleAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Context ctx = this;
        this.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.task_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.help_task) {
                    Utils.imgDialog(ctx, R.drawable.help_task);
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

            if (response == null || response.length == 0)
                return;

            String[] newLines = new String(response, StandardCharsets.UTF_8).split("\n");

            if (newLines.length != lines.length) {
                recycleAdapter.update(newLines);
                recycleAdapter.notifyItemRangeChanged(0, newLines.length);
            } else {
                recycleAdapter.update(newLines);
                for (int i = 0; i < lines.length; i++) {
                    if (!lines[i].equals(newLines[i]))
                        recycleAdapter.notifyItemChanged(i);
                }
            }
            lines = newLines;
        }
    };


    public static class AppTimeAdapter extends RecyclerView.Adapter<AppTimeAdapter.MyViewHolder> {
        private String[] lines;

        public AppTimeAdapter(String[] lines) {
            this.lines = lines;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_time_layout, parent, false);
            return new MyViewHolder(view);
        }

        @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            String line = lines[position];

            // lastUserTime, lastSysTime, userTime, sysTime;
            long[] cpuTime = new long[4];
            String[] times = line.split(" ");
            int uid = 0;
            try {
                uid = Integer.parseInt(times[0]);
                for (int i = 0; i < 4; i++)
                    cpuTime[i] = Long.parseLong(times[i + 1]);
            } catch (Exception ignored) {
            }

            AppInfoCache.Info info = AppInfoCache.get(uid);
            if (info != null) {
                holder.app_icon.setImageDrawable(info.icon);
                holder.app_label.setText(info.label);
            } else {
                holder.app_label.setText(String.valueOf(uid));
            }

            holder.userTimeSum.setText(getTimeStr(cpuTime[2]));
            holder.sysTimeSum.setText(getTimeStr(cpuTime[3]));
            holder.userTimeDelta.setText(getTimeStr(cpuTime[2] - cpuTime[0]));
            holder.sysTimeDelta.setText(getTimeStr(cpuTime[3] - cpuTime[1]));
        }

        @SuppressLint("DefaultLocale")
        String getTimeStr(long time) {
            if (time <= 0) return "";

            StringBuilder res = new StringBuilder();
            int ms = (int) (time % 1000);
            time /= 1000; // now Unit is second

            if (time >= 3600) {
                res.append(time / 3600).append('h');
                time %= 3600;
            }
            if (time >= 60) {
                res.append(time / 60).append('m');
                time %= 60;
            }
            res.append(String.format("%02d.%03ds", time, ms));

            return res.toString();
        }

        @Override
        public int getItemCount() {
            return lines.length;
        }

        public void update(String[] newlines) {
            lines = newlines;
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