package com.jark006.freezeit;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class appListRecyclerAdapter extends RecyclerView.Adapter<appListRecyclerAdapter.MyViewHolder> implements Filterable {

    private final List<ApplicationInfo> applicationList;
    private List<ApplicationInfo> applicationListFilter;
    private final HashSet<String> whiteListForce;
    private final HashSet<String> whiteListConf;
    Handler.Callback callback;
    Context context;
    PackageManager pm;
    HashMap<String, String> appName = new HashMap<>();

    public appListRecyclerAdapter(Context context, List<ApplicationInfo> applicationList,
                                  HashSet<String> whiteListForce, HashSet<String> whiteListConf, Handler.Callback callback) {
        this.applicationList = applicationList;
        this.applicationListFilter = applicationList;
        this.whiteListForce = whiteListForce;
        this.whiteListConf = whiteListConf;
        this.context = context;
        this.callback = callback;
        this.pm = context.getPackageManager();

        for (ApplicationInfo appInfo : applicationList) {
            String label = pm.getApplicationLabel(appInfo).toString();
            if (label.endsWith("Application") || label.endsWith(".xml") || label.endsWith("false"))
                label = appInfo.packageName;
            appName.put(appInfo.packageName, label);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item_layout, parent, false);
        return new MyViewHolder(view);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ApplicationInfo appInfo = applicationListFilter.get(position);


        String label = appName.get(appInfo.packageName);
        Drawable icon = appInfo.loadIcon(pm);
        holder.package_name.setText(appInfo.packageName);

        if (whiteListConf.contains(appInfo.packageName)) {
            holder.app_label.setText(label);
            holder.app_icon.setImageDrawable(icon);
        } else if (whiteListForce.contains(appInfo.packageName)) {
            holder.app_label.setText("[内置白名单] " + label);
            holder.app_icon.setImageDrawable(icon);
        } else {
            holder.app_label.setText("[黑名单] " + label);
            holder.app_icon.setImageDrawable(Utils.convertToGrayscale(icon));
        }
        if (!whiteListForce.contains(appInfo.packageName)) {
            holder.itemBackground.setOnClickListener(v -> {
//                Toast.makeText(context, "" + appInfo.packageName, Toast.LENGTH_SHORT).show();
                if (whiteListConf.contains(appInfo.packageName)) {
                    whiteListConf.remove(appInfo.packageName);
                } else {
                    whiteListConf.add(appInfo.packageName);
                }

                StringBuilder newConf = new StringBuilder();
                for (String s : whiteListConf) {
                    newConf.append(s).append('\n');
                }
                Log.i(TAG, "onBindViewHolder: " + newConf);

                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("response", newConf.toString());
                data.putInt("position", position);
                msg.setData(data);
                Log.i(TAG, "onBindViewHolder: " + newConf);
                callback.handleMessage(msg);
            });
        } else {
            holder.itemBackground.setOnClickListener(v ->
                    Toast.makeText(context, "达咩", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public int getItemCount() {
        return applicationListFilter.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ApplicationInfo> mApplicationListFilter= new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    mApplicationListFilter = applicationList;
                } else {
                    String keyWord = constraint.toString().toLowerCase();
                    for (ApplicationInfo appInfo : applicationList) {
                        String label = appName.get(appInfo.packageName);
                        if(label == null || label.length() == 0)
                            continue;

                        if (appInfo.packageName.toLowerCase().contains(keyWord) ||
                                label.toLowerCase().contains(keyWord)) {
                            mApplicationListFilter.add(appInfo);
                        }
                    }
                }
                applicationListFilter = mApplicationListFilter;
                return new FilterResults();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        LinearLayout itemBackground;
        ImageView app_icon;
        TextView app_label, package_name;

        public MyViewHolder(View view) {
            super(view);
            itemBackground = view.findViewById(R.id.app_item_background);
            app_icon = view.findViewById(R.id.app_icon);
            app_label = view.findViewById(R.id.app_label);
            package_name = view.findViewById(R.id.package_name);
        }
    }
}
