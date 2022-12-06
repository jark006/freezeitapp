package io.github.jark006.freezeit.adapter;

import static io.github.jark006.freezeit.Utils.CFG_TERMINATE;
import static io.github.jark006.freezeit.Utils.CFG_SIGSTOP;
import static io.github.jark006.freezeit.Utils.CFG_FREEZER;
import static io.github.jark006.freezeit.Utils.CFG_WHITELIST;
import static io.github.jark006.freezeit.Utils.CFG_WHITEFORCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.github.jark006.freezeit.R;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppCfgAdapter extends RecyclerView.Adapter<AppCfgAdapter.MyViewHolder> {
    private final List<ApplicationInfo> applicationList;
    private List<ApplicationInfo> applicationListFilter;
    private final HashMap<Integer, Pair<Integer, Integer>> appCfg;

    Context context;
    PackageManager pm;

    public AppCfgAdapter(Context context, List<ApplicationInfo> applicationList,
                         HashMap<Integer, Pair<Integer, Integer>> appCfg) {
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

    int cfgValue2idx(int i) {
        switch (i) {
            case CFG_TERMINATE:  return 0;
            case CFG_SIGSTOP:    return 1;
            case CFG_FREEZER:
            default:             return 2;
            case CFG_WHITELIST:  return 3;
        }
    }

    int idx2cfgValue(int i) {
        switch (i) {
            case 0:  return CFG_TERMINATE;
            case 1:  return CFG_SIGSTOP;
            case 2:
            default: return CFG_FREEZER;
            case 3:  return CFG_WHITELIST;
        }
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ApplicationInfo appInfo = applicationListFilter.get(position);

        String label = pm.getApplicationLabel(appInfo).toString();
        holder.package_name.setText(appInfo.packageName);
        holder.app_label.setText(label);
        holder.app_icon.setImageDrawable(appInfo.loadIcon(pm));

        Pair<Integer, Integer> cfg = appCfg.get(appInfo.uid);
        if (cfg == null) cfg = new Pair<>(CFG_FREEZER, 0);

        if (cfg.first.equals(CFG_WHITEFORCE)) {
            holder.spinner_tolerant.setVisibility(View.GONE);
            holder.spinner_cfg.setVisibility(View.GONE);
            return;
        }

        holder.spinner_cfg.setVisibility(View.VISIBLE);
        if (cfg.first >= CFG_WHITELIST) holder.spinner_tolerant.setVisibility(View.INVISIBLE);
        else holder.spinner_tolerant.setVisibility(View.VISIBLE);

        holder.spinner_cfg.setSelection(cfgValue2idx(cfg.first));
        holder.spinner_tolerant.setSelection(cfg.second == 0 ? 0 : 1);

        Pair<Integer, Integer> finalCfg = cfg;
        holder.spinner_cfg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                int cfgValue = idx2cfgValue(spinnerPosition);
                int isTolerant = finalCfg.second;
                appCfg.put(appInfo.uid, new Pair<>(cfgValue, isTolerant));

                if (cfgValue >= CFG_WHITELIST) holder.spinner_tolerant.setVisibility(View.INVISIBLE);
                else holder.spinner_tolerant.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        holder.spinner_tolerant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                int cfgInt = finalCfg.first;
                appCfg.put(appInfo.uid, new Pair<>(cfgInt, spinnerPosition));
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

                if (appInfo.packageName.toLowerCase().contains(keyWord)) {
                    applicationListFilter.add(appInfo);
                    continue;
                }

                String label = pm.getApplicationLabel(appInfo).toString().toLowerCase();
                if (label.contains(keyWord)) {
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
        Spinner spinner_cfg, spinner_tolerant;

        public MyViewHolder(View view) {
            super(view);
            item = view.findViewById(R.id.app_item_background);
            app_icon = view.findViewById(R.id.app_icon);
            app_label = view.findViewById(R.id.app_label);
            package_name = view.findViewById(R.id.package_name);
            spinner_cfg = view.findViewById(R.id.spinner_cfg);
            spinner_tolerant = view.findViewById(R.id.spinner_level);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void convertCfg() {
        appCfg.forEach((key, value) -> {
            if (value.first == CFG_FREEZER)
                appCfg.put(key, new Pair<>(CFG_SIGSTOP, value.second));
            else if (value.first == CFG_SIGSTOP)
                appCfg.put(key, new Pair<>(CFG_FREEZER, value.second));
        });
        notifyItemRangeChanged(0, appCfg.size());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void convertTolerant() {
        appCfg.forEach((key, value) -> {
            if (value.second == 0)
                appCfg.put(key, new Pair<>(value.first, 1));
            else
                appCfg.put(key, new Pair<>(value.first, 0));
        });
        notifyItemRangeChanged(0, appCfg.size());
    }


    public byte[] getCfgBytes() {
        StringBuilder tmp = new StringBuilder();

        appCfg.forEach((key, value) -> {
            if (value.first < CFG_WHITEFORCE)
                tmp.append(key).append(' ').append(value.first).append(' ').append(value.second).append('\n');
        });

        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }
}
