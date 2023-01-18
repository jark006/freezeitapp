package io.github.jark006.freezeit.fragment;

import static io.github.jark006.freezeit.Utils.CFG_FREEZER;
import static io.github.jark006.freezeit.Utils.CFG_SIGSTOP;
import static io.github.jark006.freezeit.Utils.CFG_TERMINATE;
import static io.github.jark006.freezeit.Utils.CFG_WHITEFORCE;
import static io.github.jark006.freezeit.Utils.CFG_WHITELIST;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import io.github.jark006.freezeit.AppInfoCache;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {
    private final static String TAG = "ConfigFragment";

    private FragmentConfigBinding binding;
    AppCfgAdapter recycleAdapter;
    final ArrayList<Integer> uidList = new ArrayList<>();
    long lastTimestamp = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        // 下拉刷新时，先更新应用缓存
        binding.swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            AppInfoCache.refreshCache(requireContext());
            AppInfoCache.getUidList(uidList);
            Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler);
        }).start());

        AppInfoCache.getUidList(uidList);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.config_menu, menu);
                SearchView searchView = (SearchView) menu.findItem(R.id.search_view).getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {//按下搜索触发
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            if (recycleAdapter != null)
                                recycleAdapter.filter(newText);
                            return true;
                        }
                    });
                } else {
                    Log.e(TAG, "onCreateMenu: searchView == null");
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.help_config) {
                    Utils.imgDialog(requireContext(), R.drawable.help_config);
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        binding.fabSave.setOnClickListener(view -> {
            if ((System.currentTimeMillis() - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = System.currentTimeMillis();
            if (recycleAdapter != null) {
                byte[] newConf = recycleAdapter.getCfgBytes();
                new Thread(() -> Utils.freezeitTask(Utils.setAppCfg, newConf, setAppCfgHandler)).start();
            }
        });

        binding.fabConvertCfg.setOnClickListener(view -> {
            if ((System.currentTimeMillis() - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = System.currentTimeMillis();

            if (recycleAdapter != null) {
                recycleAdapter.convertCfg();
            }
        });

        binding.fabConvertTolerant.setOnClickListener(view -> {
            if ((System.currentTimeMillis() - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = System.currentTimeMillis();

            if (recycleAdapter != null) {
                recycleAdapter.convertTolerant();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread(() -> Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler)).start();
    }

    private final Handler getAppCfgHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                binding.swipeRefreshLayout.setRefreshing(false);
                return;
            }

            // 配置名单 <uid, <freezeMode, isTolerant>>
            // freezeMode: [10]:杀死 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
            HashMap<Integer, Pair<Integer, Integer>> appCfg = new HashMap<>();

            String[] list = new String(response, StandardCharsets.UTF_8).split("\n");
            for (String item : list) {
                String[] package_mode = item.split(" ");

                if (package_mode.length != 3) {
                    Log.e(TAG, "handleMessage: unknownItem:[" + item + "]");
                    continue;
                }

                try {
                    appCfg.put(Integer.parseInt(package_mode[0]), new Pair<>(
                            Integer.parseInt(package_mode[1]), Integer.parseInt(package_mode[2])));
                } catch (Exception e) {
                    Log.e(TAG, "handleMessage: unknownItem:[" + item + "]");
                }
            }

            // 补全
            uidList.forEach(uid -> {
                if (!appCfg.containsKey(uid))
                    appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, 1)); // 默认Freezer 宽松
            });
            // 检查非法配置
            appCfg.forEach((uid, cfg) -> {
                if (cfg.first < Utils.CFG_TERMINATE || cfg.first > Utils.CFG_WHITEFORCE)
                    appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, cfg.second));
            });

            // [10]:杀死后台 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
            ArrayList<Integer> uidListSort = new ArrayList<>();

            // 先排 自由
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_WHITELIST)
                    uidListSort.add(uid);
            }

            // 优先排列：FREEZER SIGSTOP 杀死后台， 次排列：宽松 严格
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_FREEZER && mode.second != 0)
                    uidListSort.add(uid);
            }
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_FREEZER && mode.second == 0)
                    uidListSort.add(uid);
            }

            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_SIGSTOP && mode.second != 0)
                    uidListSort.add(uid);
            }
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_SIGSTOP && mode.second == 0)
                    uidListSort.add(uid);
            }

            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_TERMINATE && mode.second != 0)
                    uidListSort.add(uid);
            }
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_TERMINATE && mode.second == 0)
                    uidListSort.add(uid);
            }

            // 最后排 内置自由
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_WHITEFORCE)
                    uidListSort.add(uid);
            }

            recycleAdapter = new AppCfgAdapter(uidListSort, appCfg);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setOrientation(RecyclerView.VERTICAL);

            binding.recyclerviewApp.setLayoutManager(layoutManager);
            binding.recyclerviewApp.setAdapter(recycleAdapter);
            binding.recyclerviewApp.setItemAnimator(new DefaultItemAnimator());

            binding.swipeRefreshLayout.setRefreshing(false);
        }
    };

    private final Handler setAppCfgHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Toast.makeText(getContext(), R.string.update_success, Toast.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]";
                Toast.makeText(getContext(), errorTips, Toast.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
            }
        }
    };


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public static class AppCfgAdapter extends RecyclerView.Adapter<AppCfgAdapter.MyViewHolder> {
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
                holder.package_name.setText("未知 Unknown");
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
                    holder.spinner_tolerant.setVisibility(cfgValue >= CFG_WHITELIST ? View.GONE : View.VISIBLE);
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

            ImageView app_icon;
            TextView app_label, package_name;
            Spinner spinner_cfg, spinner_tolerant;

            public MyViewHolder(View view) {
                super(view);
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

}