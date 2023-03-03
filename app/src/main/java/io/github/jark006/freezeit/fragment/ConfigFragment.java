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
        binding.swipeRefreshLayout.setRefreshing(true);

        // 下拉刷新时，先更新应用缓存
        binding.swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            AppInfoCache.refreshCache(requireContext());
            AppInfoCache.getUidList(uidList);
            Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler);
        }).start());

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
                                recycleAdapter.filter(newText != null ? newText.toLowerCase() : "");
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
                    Utils.layoutDialog(requireContext(), R.layout.help_dialog_config);
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        binding.fabSave.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            if (recycleAdapter != null) {
                byte[] newConf = recycleAdapter.getCfgBytes();
                new Thread(() -> Utils.freezeitTask(Utils.setAppCfg, newConf, setAppCfgHandler)).start();
            }
        });

        binding.fabConvertCfg.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            if (recycleAdapter != null) {
                recycleAdapter.convertCfg();
            }
        });

        binding.fabConvertTolerant.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            if (recycleAdapter != null) {
                recycleAdapter.convertTolerant();
            }
        });

        binding.fabSwitchSys.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 1000) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            if (recycleAdapter != null) {
                recycleAdapter.switchAppType();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread(() -> {
            AppInfoCache.getUidList(uidList);
            Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler);
        }).start();
    }

    private final Handler getAppCfgHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0 || response.length % 12 != 0) {
                if (binding != null)
                    binding.swipeRefreshLayout.setRefreshing(false);
                return;
            }

            // 配置名单 <uid, <freezeMode, isTolerant>>
            // freezeMode: [10]:杀死 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
            HashMap<Integer, Pair<Integer, Integer>> appCfg = new HashMap<>();

            // 每个配置含：3个[int32]数据，12字节 小端
            for (int i = 0; i < response.length; i += 12) {
                int uid = Utils.Byte2Int(response, i);
                int freezeMode = Utils.Byte2Int(response, i + 4);
                int isTolerant = Utils.Byte2Int(response, i + 8);
                appCfg.put(uid, new Pair<>(freezeMode, isTolerant));
            }

            // 补全
            uidList.forEach(uid -> {
                if (!appCfg.containsKey(uid))
                    appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, 1)); // 默认Freezer 宽松
            });
            // 检查非法配置
            appCfg.forEach((uid, cfg) -> {
                if (!Utils.CFG_SET.contains(cfg.first))
                    appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, cfg.second));
            });

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

            if (binding == null) return;

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

            String res = new String(response);
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
        private final ArrayList<Integer> uidListFilter = new ArrayList<>(200);
        private final HashMap<Integer, Pair<Integer, Integer>> appCfg; //<uid, <freezeMode, tolerant>>
        boolean showSystemApp = false;

        public AppCfgAdapter(ArrayList<Integer> uidList, HashMap<Integer, Pair<Integer, Integer>> appCfg) {
            this.uidList = uidList;
            this.appCfg = appCfg;

            for (int uid : uidList) {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp)
                    uidListFilter.add(uid);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.app_cfg_layout, parent, false);
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
            } else {
                holder.app_label.setText(String.valueOf(uid));
            }

            var cfg = appCfg.get(uid);
            int freezeMode = cfg == null ? CFG_FREEZER : cfg.first;
            int isTolerant = cfg == null ? 0 : cfg.second;

            if (freezeMode == CFG_WHITEFORCE) {
                holder.spinner_tolerant.setVisibility(View.GONE);
                holder.spinner_cfg.setVisibility(View.GONE);
                return;
            }

            holder.spinner_cfg.setVisibility(View.VISIBLE);
            holder.spinner_tolerant.setVisibility(freezeMode == CFG_WHITELIST ? View.GONE : View.VISIBLE);

            holder.spinner_cfg.setSelection(cfgValue2idx(freezeMode));
            holder.spinner_tolerant.setSelection(isTolerant == 0 ? 0 : 1);

            holder.spinner_cfg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                    var cfg = appCfg.get(uid);
                    if (cfg == null) return;
                    int newFreezeMode = idx2cfgValue(spinnerPosition);
                    appCfg.put(uid, new Pair<>(newFreezeMode, cfg.second));
                    holder.spinner_tolerant.setVisibility(newFreezeMode == CFG_WHITELIST ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            holder.spinner_tolerant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                    var cfg = appCfg.get(uid);
                    if (cfg == null) return;
                    appCfg.put(uid, new Pair<>(cfg.first, spinnerPosition));
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
        public void filter(@NonNull final String keyWord) {
            uidListFilter.clear();
            for (int uid : uidList) {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp &&
                        (keyWord.isEmpty() || AppInfoCache.get(uid).contains(keyWord)))
                    uidListFilter.add(uid);
            }
            notifyDataSetChanged();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView app_icon;
            TextView app_label;
            Spinner spinner_cfg, spinner_tolerant;

            public MyViewHolder(View view) {
                super(view);
                app_icon = view.findViewById(R.id.app_icon);
                app_label = view.findViewById(R.id.app_label);
                spinner_cfg = view.findViewById(R.id.spinner_cfg);
                spinner_tolerant = view.findViewById(R.id.spinner_level);
            }
        }

        public void convertCfg() {
            appCfg.forEach((uid, cfg) -> {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp) {
                    if (cfg.first == CFG_FREEZER)
                        appCfg.put(uid, new Pair<>(CFG_SIGSTOP, cfg.second));
                    else if (cfg.first == CFG_SIGSTOP)
                        appCfg.put(uid, new Pair<>(CFG_FREEZER, cfg.second));
                }
            });
            notifyItemRangeChanged(0, uidListFilter.size());
        }

        public void convertTolerant() {
            appCfg.forEach((uid, cfg) -> {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp) {
                    if (cfg.second == 0)
                        appCfg.put(uid, new Pair<>(cfg.first, 1));
                    else
                        appCfg.put(uid, new Pair<>(cfg.first, 0));
                }
            });
            notifyItemRangeChanged(0, uidListFilter.size());
        }


        public byte[] getCfgBytes() {

            if (appCfg.isEmpty()) return null;

            byte[] tmp = new byte[appCfg.size() * 12];
            final int[] idx = {0};
            appCfg.forEach((uid, cfg) -> {
                Utils.Int2Byte(uid, tmp, idx[0]);
                idx[0] += 4;
                Utils.Int2Byte(cfg.first, tmp, idx[0]);
                idx[0] += 4;
                Utils.Int2Byte(cfg.second, tmp, idx[0]);
                idx[0] += 4;
            });
            return tmp;
        }

        @SuppressLint("NotifyDataSetChanged")
        public void switchAppType() {
            showSystemApp = !showSystemApp;

            uidListFilter.clear();
            for (int uid : uidList) {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp)
                    uidListFilter.add(uid);
            }
            notifyDataSetChanged();
        }
    }

}