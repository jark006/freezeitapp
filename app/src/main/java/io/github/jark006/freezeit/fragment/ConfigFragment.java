package io.github.jark006.freezeit.fragment;

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
import io.github.jark006.freezeit.adapter.AppCfgAdapter;
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

            if (response == null || response.length == 0)
                return;

            // 配置名单 <uid, <cfg, isTolerant>>
            // cfg: [10]:杀死 [20]:SIGSTOP [30]:Freezer [40]:自由 [50]:内置
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

            // 再排序 宽松前台 的 FREEZER SIGSTOP 杀死后台
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first <= Utils.CFG_FREEZER && mode.second != 0)
                    uidListSort.add(uid);
            }

            // 再排序 严格前台 的 FREEZER SIGSTOP 杀死后台
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_FREEZER && mode.second == 0)
                    uidListSort.add(uid);
            }
            for (int uid : uidList) {
                Pair<Integer, Integer> mode = appCfg.get(uid);
                if (mode != null && mode.first == Utils.CFG_SIGSTOP && mode.second == 0)
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
}