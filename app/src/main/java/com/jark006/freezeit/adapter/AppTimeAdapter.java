package com.jark006.freezeit.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jark006.freezeit.R;

import java.util.HashMap;
import java.util.List;

public class AppTimeAdapter extends RecyclerView.Adapter<AppTimeAdapter.MyViewHolder> {
    private final HashMap<Integer, ApplicationInfo> applicationMap = new HashMap<>();
    private String[] lines;
    Context context;
    PackageManager pm;

    public AppTimeAdapter(Context context, String[] lines) {
        this.lines = lines;
        this.context = context;
        this.pm = context.getPackageManager();

        List<ApplicationInfo> applicationList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationList = pm.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES));
        } else {
            applicationList = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        }

        for (ApplicationInfo info : applicationList) {
            if (info.uid > 10000) {
                applicationMap.put(info.uid, info);
            }
        }
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

        if (uid == 0) {
            holder.app_label.setText("UID:" + uid);
            holder.package_name.setText("null");
            return;
        }
        ApplicationInfo appInfo = applicationMap.get(uid);
        if (appInfo != null) {
            holder.app_icon.setImageDrawable(appInfo.loadIcon(pm));
            holder.package_name.setText(appInfo.packageName);
            holder.app_label.setText(pm.getApplicationLabel(appInfo).toString());
        }


        StringBuilder userTime = getTimeStr(cpuTime[2], false);
        StringBuilder sysTime = getTimeStr(cpuTime[3], false);

        StringBuilder userTimeDelta = getTimeStr(cpuTime[2] - cpuTime[0], true);
        StringBuilder sysTimeDelta = getTimeStr(cpuTime[3] - cpuTime[1], true);

        holder.userTimeSum.setText(userTime);
        holder.sysTimeSum.setText(sysTime);
        holder.userTimeDelta.setText(userTimeDelta);
        holder.sysTimeDelta.setText(sysTimeDelta);
    }

    @SuppressLint("DefaultLocale")
    StringBuilder getTimeStr(long time, boolean addPlus) {
        StringBuilder res = new StringBuilder();
        if (time < 0) {
            res.append('-');
            time = -(time);
        } else if (time == 0) {
            return res;
        } else if (addPlus) {
            res.append('+');
        }

        if (time > 60 * 1000) {
            res.append(time / (60 * 1000)).append("分");
            time %= (60 * 1000);
        }
        res.append(String.format("%02d.%03d", time / 1000, time % 1000));

        return res;
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
        TextView app_label, package_name, userTimeDelta, userTimeSum, sysTimeDelta, sysTimeSum;


        public MyViewHolder(View view) {
            super(view);

            app_icon = view.findViewById(R.id.app_icon);
            app_label = view.findViewById(R.id.app_label);
            package_name = view.findViewById(R.id.package_name);

            userTimeDelta = view.findViewById(R.id.userTimeDelta);
            userTimeSum = view.findViewById(R.id.userTimeSum);
            sysTimeDelta = view.findViewById(R.id.sysTimeDelta);
            sysTimeSum = view.findViewById(R.id.sysTimeSum);

        }
    }
}
