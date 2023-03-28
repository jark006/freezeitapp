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
import java.util.Locale;

import io.github.jark006.freezeit.AppInfoCache;
import io.github.jark006.freezeit.R;
import io.github.jark006.freezeit.StaticData;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {
    private final static String TAG = "ConfigFragment";
    final int GET_APP_CFG = 1,
            SET_CFG_SUCCESS = 2,
            SET_CFG_FAIL = 3;

    private FragmentConfigBinding binding;
    AppCfgAdapter recycleAdapter = new AppCfgAdapter();
    long lastTimestamp = 0;


    // 配置名单 <uid, <freezeMode, isTolerant>>
    // freezeMode: [10]:杀死 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
    HashMap<Integer, Pair<Integer, Integer>> appCfg = new HashMap<>();
    ArrayList<Integer> uidListSort = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        binding.recyclerviewApp.setLayoutManager(new LinearLayoutManager(requireContext()));
        var animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        binding.recyclerviewApp.setItemAnimator(animator);
        binding.recyclerviewApp.setAdapter(recycleAdapter);
        binding.recyclerviewApp.setHasFixedSize(true);

        binding.swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            AppInfoCache.refreshCache(requireContext());// 下拉刷新时，先更新应用缓存
            getAppCfgTask();
        }).start());

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.config_menu, menu);
                SearchView searchView = (SearchView) menu.findItem(R.id.search_view).getActionView();
                if (searchView == null) {
                    Log.e(TAG, "onCreateMenu: searchView == null");
                    return;
                }
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {//按下搜索触发
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        recycleAdapter.filter(newText != null ? newText.toLowerCase(Locale.ENGLISH) : "");
                        return true;
                    }
                });

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

            byte[] newConf = recycleAdapter.getCfgBytes();
            if (newConf == null) return;

            new Thread(() -> {
                var recvLen = Utils.freezeitTask(Utils.setAppCfg, newConf);
                handler.sendEmptyMessage((recvLen == 7 &&
                        new String(StaticData.response, 0, 7).equals("success")) ?
                        SET_CFG_SUCCESS : SET_CFG_FAIL);
            }).start();
        });

        binding.fabConvertCfg.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            recycleAdapter.convertCfg();
        });

        binding.fabConvertTolerant.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            recycleAdapter.convertTolerant();
        });

        binding.fabSwitchSys.setOnClickListener(view -> {
            var now = System.currentTimeMillis();
            if ((now - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = now;

            recycleAdapter.switchAppType();
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        binding.swipeRefreshLayout.setRefreshing(true);
        new Thread(this::getAppCfgTask).start();
    }

    void getAppCfgTask() {
        var recvLen = Utils.freezeitTask(Utils.getAppCfg, null);
        if (recvLen == 0 || recvLen % 12 != 0) {
            if (binding != null)
                binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        appCfg.clear();
        // 每个配置含：3个[int32]数据，12字节 小端
        for (int i = 0; i < recvLen; i += 12) {
            int uid = Utils.Byte2Int(StaticData.response, i);
            int freezeMode = Utils.Byte2Int(StaticData.response, i + 4);
            int isTolerant = Utils.Byte2Int(StaticData.response, i + 8);
            if (AppInfoCache.contains(uid)) // 冻它底层可以获取全部应用，但应用层获取的 AppInfoCache 可能会缺一些特殊应用
                appCfg.put(uid, new Pair<>(freezeMode, isTolerant));
        }

        var uidList = AppInfoCache.getUidList();
        // 补全  此时 uidList 可能包含一些刚刚安装的应用，而底层还没更新全部应用列表
        uidList.forEach(uid -> {
            if (!appCfg.containsKey(uid))
                appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, 1)); // 默认Freezer 宽松
        });
        // 检查非法配置
        appCfg.forEach((uid, cfg) -> {
            if (!Utils.CFG_SET.contains(cfg.first))
                appCfg.put(uid, new Pair<>(Utils.CFG_FREEZER, cfg.second));
        });

        uidListSort.clear();

        // 先排 自由
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_WHITELIST)
                uidListSort.add(uid);
        }

        // 优先排列：FREEZER SIGSTOP 杀死后台， 次排列：宽松 严格
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_FREEZER && mode.second != 0)
                uidListSort.add(uid);
        }
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_FREEZER && mode.second == 0)
                uidListSort.add(uid);
        }

        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_SIGSTOP && mode.second != 0)
                uidListSort.add(uid);
        }
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_SIGSTOP && mode.second == 0)
                uidListSort.add(uid);
        }

        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_TERMINATE && mode.second != 0)
                uidListSort.add(uid);
        }
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_TERMINATE && mode.second == 0)
                uidListSort.add(uid);
        }

        // 最后排 内置自由
        for (int uid : uidList) {
            var mode = appCfg.get(uid);
            if (mode != null && mode.first == Utils.CFG_WHITEFORCE)
                uidListSort.add(uid);
        }

        handler.sendEmptyMessage(GET_APP_CFG);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (binding == null)
                return;

            switch (msg.what) {
                case GET_APP_CFG:
                    recycleAdapter.updateDataSet(uidListSort, appCfg);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    break;
                case SET_CFG_SUCCESS:
                    Toast.makeText(requireContext(), R.string.update_success, Toast.LENGTH_SHORT).show();
                    break;
                case SET_CFG_FAIL:
                    Toast.makeText(requireContext(), R.string.update_fail, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    static class AppCfgAdapter extends RecyclerView.Adapter<AppCfgAdapter.MyViewHolder> {
        ArrayList<Integer> uidList = new ArrayList<>();
        ArrayList<Integer> uidListFilter = new ArrayList<>(400);
        static HashMap<Integer, Pair<Integer, Integer>> appCfg = new HashMap<>(); //<uid, <freezeMode, tolerant>>
        boolean showSystemApp = false;
        String keyWord = "";

        public AppCfgAdapter() {
        }

        public void updateDataSet(@NonNull ArrayList<Integer> newUidList,
                                  @NonNull HashMap<Integer, Pair<Integer, Integer>> newAppCfg) {
            uidList = newUidList;
            appCfg.clear();
            appCfg.putAll(newAppCfg);
            keyWord = "";
            updateAndRefreshView();
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

        static int idx2cfgValue(int i) {
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
        }

        @Override
        public int getItemCount() {
            return uidListFilter.size();
        }

        static class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView app_icon;
            TextView app_label;
            Spinner spinner_cfg, spinner_tolerant;
            int uid = 0;

            public MyViewHolder(View view) {
                super(view);
                app_icon = view.findViewById(R.id.app_icon);
                app_label = view.findViewById(R.id.app_label);
                spinner_cfg = view.findViewById(R.id.spinner_cfg);
                spinner_tolerant = view.findViewById(R.id.spinner_level);

                spinner_cfg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                        if (uid == 0) return;
                        var cfg = appCfg.get(uid);
                        int newFreezeMode = idx2cfgValue(spinnerPosition);
                        if (cfg == null || cfg.first == newFreezeMode) return;
                        appCfg.put(uid, new Pair<>(newFreezeMode, cfg.second));
                        spinner_tolerant.setVisibility(newFreezeMode == CFG_WHITELIST ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                spinner_tolerant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                        if (uid == 0) return;
                        var cfg = appCfg.get(uid);
                        if (cfg == null || cfg.second == spinnerPosition) return;
                        appCfg.put(uid, new Pair<>(cfg.first, spinnerPosition));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }

        }

        @SuppressLint("NotifyDataSetChanged")
        public void convertCfg() {
            appCfg.forEach((uid, cfg) -> {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp) {
                    if (cfg.first == CFG_FREEZER)
                        appCfg.put(uid, new Pair<>(CFG_SIGSTOP, cfg.second));
                    else if (cfg.first == CFG_SIGSTOP)
                        appCfg.put(uid, new Pair<>(CFG_FREEZER, cfg.second));
                }
            });
            notifyDataSetChanged();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void convertTolerant() {
            appCfg.forEach((uid, cfg) -> {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp) {
                    if (cfg.second == 0)
                        appCfg.put(uid, new Pair<>(cfg.first, 1));
                    else
                        appCfg.put(uid, new Pair<>(cfg.first, 0));
                }
            });
            notifyDataSetChanged();
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

        public void switchAppType() {
            showSystemApp = !showSystemApp;
            updateAndRefreshView();
        }

        public void filter(@NonNull final String _keyWord) {
            keyWord = _keyWord;
            updateAndRefreshView();
        }

        @SuppressLint("NotifyDataSetChanged")
        void updateAndRefreshView() {
            uidListFilter.clear();
            for (int uid : uidList) {
                if (AppInfoCache.get(uid).isSystemApp == showSystemApp &&
                        (keyWord.isEmpty() || AppInfoCache.get(uid).contains(keyWord)))
                    uidListFilter.add(uid);
            }
            notifyDataSetChanged();
        }
    }

}