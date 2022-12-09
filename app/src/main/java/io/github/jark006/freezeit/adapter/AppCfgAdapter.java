package io.github.jark006.freezeit.adapter;

import static io.github.jark006.freezeit.Utils.CFG_FREEZER;
import static io.github.jark006.freezeit.Utils.CFG_SIGSTOP;
import static io.github.jark006.freezeit.Utils.CFG_TERMINATE;
import static io.github.jark006.freezeit.Utils.CFG_WHITEFORCE;
import static io.github.jark006.freezeit.Utils.CFG_WHITELIST;

import android.annotation.SuppressLint;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import io.github.jark006.freezeit.AppInfoCache;
import io.github.jark006.freezeit.R;

public class AppCfgAdapter extends RecyclerView.Adapter<AppCfgAdapter.MyViewHolder> {
    private final ArrayList<Integer> uidList;
    private ArrayList<Integer> uidListFilter;
    private final HashMap<Integer, Pair<Integer, Integer>> appCfg; //<uid, <cfg, tolerant>>

    public AppCfgAdapter(ArrayList<Integer> uidList, HashMap<Integer, Pair<Integer, Integer>> appCfg) {
        this.uidList = uidList;
        this.uidListFilter = uidList;
        this.appCfg = appCfg;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_cfg_layout, parent, false);
        return new MyViewHolder(view);
    }

    int cfgValue2idx(int i) {
        switch (i) {
            case CFG_TERMINATE:
                return 0;
            case CFG_SIGSTOP:
                return 1;
            case CFG_FREEZER:
            default:
                return 2;
            case CFG_WHITELIST:
                return 3;
        }
    }

    int idx2cfgValue(int i) {
        switch (i) {
            case 0:
                return CFG_TERMINATE;
            case 1:
                return CFG_SIGSTOP;
            case 2:
            default:
                return CFG_FREEZER;
            case 3:
                return CFG_WHITELIST;
        }
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        int uid = uidListFilter.get(position);

        AppInfoCache.Info info = AppInfoCache.get(uid);
        if (info != null) {
            holder.app_icon.setImageDrawable(info.icon);
            holder.app_label.setText(info.label);
            holder.package_name.setText(info.packName);
        } else {
            holder.package_name.setText("未知");
            holder.app_label.setText("UID:" + uid);
        }

        Pair<Integer, Integer> cfg = appCfg.get(uid);
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
                appCfg.put(uid, new Pair<>(cfgValue, isTolerant));

                if (cfgValue >= CFG_WHITELIST)
                    holder.spinner_tolerant.setVisibility(View.INVISIBLE);
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
                appCfg.put(uid, new Pair<>(cfgInt, spinnerPosition));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return uidListFilter.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String keyWord) {
        if (keyWord == null || keyWord.length() == 0) {
            uidListFilter = uidList;
        } else {
            keyWord = keyWord.toLowerCase();
            uidListFilter = new ArrayList<>();
            for (int uid : uidList) {
                if (AppInfoCache.get(uid).forSearch.contains(keyWord))
                    uidListFilter.add(uid);
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

        appCfg.forEach((uid, cfg) -> {
            if (cfg.first < CFG_WHITEFORCE)
                tmp.append(uid).append(' ').append(cfg.first).append(' ').append(cfg.second).append('\n');
        });

        return tmp.toString().getBytes(StandardCharsets.UTF_8);
    }
}
