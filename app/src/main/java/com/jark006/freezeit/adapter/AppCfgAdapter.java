package com.jark006.freezeit.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jark006.freezeit.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppCfgAdapter extends RecyclerView.Adapter<AppCfgAdapter.MyViewHolder> {
    private final List<ApplicationInfo> applicationList;
    private List<ApplicationInfo> applicationListFilter;
    private final HashMap<String, Integer> appCfg;

    Context context;
    PackageManager pm;

    public AppCfgAdapter(Context context, List<ApplicationInfo> applicationList, HashMap<String, Integer> appCfg) {
        this.applicationList = applicationList;
        this.applicationListFilter = applicationList;
        this.appCfg = appCfg;
        this.context = context;
        this.pm = context.getPackageManager();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_cfg_layout, parent, false);
        return new MyViewHolder(view);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ApplicationInfo appInfo = applicationListFilter.get(position);

        String label = pm.getApplicationLabel(appInfo).toString();
        holder.package_name.setText(appInfo.packageName);
        holder.app_label.setText(label);
        holder.app_icon.setImageDrawable(appInfo.loadIcon(pm));

        Integer mode = appCfg.get(appInfo.packageName);

        if (mode != null && mode.equals(3)) {
            holder.spinner.setVisibility(View.GONE);
            return;
        }

        holder.spinner.setVisibility(View.VISIBLE);

        if (mode != null && mode >= -2 && mode <= 3)
            holder.spinner.setSelection(mode + 2);
        else
            holder.spinner.setSelection(2);

        holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {

                // [-2]:杀死 [-1]:SIGSTOP [0]:freezer [1]:动态 [2]:配置 [3]:内置
                int modeInt = spinnerPosition - 2;
                if (modeInt < -2 || modeInt > 2) {
                    Toast.makeText(context, "达咩", Toast.LENGTH_SHORT).show();
                    return;
                }
                appCfg.put(appInfo.packageName, modeInt);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return applicationListFilter.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String keyWord) {
        if (keyWord == null || keyWord.length() == 0) {
            applicationListFilter = applicationList;
        } else {
            keyWord = keyWord.toLowerCase();
            applicationListFilter = new ArrayList<>();
            for (ApplicationInfo appInfo : applicationList) {
                String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();

                if (appInfo.packageName.toLowerCase().contains(keyWord) ||
                        label.contains(keyWord)) {
                    applicationListFilter.add(appInfo);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        LinearLayout item;
        ImageView app_icon;
        TextView app_label, package_name;
        Spinner spinner;

        public MyViewHolder(View view) {
            super(view);
            item = view.findViewById(R.id.app_item_background);
            app_icon = view.findViewById(R.id.app_icon);
            app_label = view.findViewById(R.id.app_label);
            package_name = view.findViewById(R.id.package_name);
            spinner = view.findViewById(R.id.spinner);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void convert() {
        appCfg.forEach((key, value) -> {
            if (value == 0) appCfg.put(key, -1);
            else if (value == -1) appCfg.put(key, 0);
        });
        notifyItemRangeChanged(0, appCfg.size());
    }


    public byte[] getCfgBytes() {
        StringBuilder tmp = new StringBuilder();

        appCfg.forEach((key, value) -> {
            if (value != 3)
                tmp.append(key).append(' ').append(value).append('\n');
        });

        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }
}
