package com.jark006.freezeit.fragment;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.jark006.freezeit.R;
import com.jark006.freezeit.Utils;
import com.jark006.freezeit.adapter.AppCfgAdapter;
import com.jark006.freezeit.databinding.FragmentConfigBinding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//                            _ooOoo_
//                           o8888888o
//                           88" . "88
//                           (| -_- |)
//                           O\  =  /O
//                        ____/`---'\____
//                      .'  \\|     |//  `.
//                     /  \\|||  :  |||//  \
//                    /  _||||| -:- |||||-  \
//                    |   | \\\  -  /// |   |
//                    | \_|  ''\---/''  |   |
//                    \  .-\__  `-`  ___/-. /
//                  ___`. .'  /--.--\  `. . __
//               ."" '<  `.___\_<|>_/___.'  >'"".
//              | | :  `- \`.;`\ _ /`;.`/ - ` : | |
//              \  \ `-.   \_ __\ /__ _/   .-` /  /
//         ======`-.____`-.___\_____/___.-`____.-'======
//                            `=---='
//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                     佛祖保佑，代码永无BUG，阿弥陀佛

public class ConfigFragment extends Fragment {
    private final static String TAG = "ConfigFragment";

    private FragmentConfigBinding binding;
    SearchView searchView;
    ConstraintLayout constraintLayout;
    AppCfgAdapter recycleAdapter;
    List<ApplicationInfo> applicationInfoList = new ArrayList<>();
    List<ApplicationInfo> applicationInfoListSort;


    //配置名单 <package, action> [-2]:杀死 [-1]:SIGSTOP [0]:freezer [1]:动态 [2]:配置 [3]:内置
    HashMap<String, Integer> appCfg = new HashMap<>();

    long lastTimestamp = 0;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        constraintLayout = binding.constraintLayoutConfig;
        recyclerView = binding.recyclerviewApp;
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler)).start());

        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> allApplicationInfoList = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : allApplicationInfoList) {
            if (appInfo.uid < 10000)
                continue;
            if ((appInfo.flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0)
                continue;
            applicationInfoList.add(appInfo);
        }

        requireActivity().addMenuProvider(new MenuProvider() {

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.config_menu, menu);

                MenuItem searchItem = menu.findItem(R.id.search_view);
                searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {//按下搜索触发
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            if (recycleAdapter != null) {
                                recycleAdapter.filter(newText);
                            }
                            return false;
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

        binding.fabConvert.setOnClickListener(view -> {
            if ((System.currentTimeMillis() - lastTimestamp) < 500) {
                Toast.makeText(requireContext(), getString(R.string.slowly_tips), Toast.LENGTH_LONG).show();
                return;
            }
            lastTimestamp = System.currentTimeMillis();

            if (recycleAdapter != null) {
                recycleAdapter.convert();
            }
        });


        new Thread(() -> Utils.freezeitTask(Utils.getAppCfg, null, getAppCfgHandler)).start();

        return binding.getRoot();
    }

    private final Handler getAppCfgHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0)
                return;

            appCfg.clear();
            String[] list = new String(response, StandardCharsets.UTF_8).split("\n");
            for (String item : list) {
                String[] package_mode = item.split(" ");

                if (package_mode.length != 2) {
                    Log.e(TAG, "handleMessage: unknownItem:[" + item + "]");
                    continue;
                }

                try {
                    appCfg.put(package_mode[0], Integer.parseInt(package_mode[1]));
                } catch (Exception e) {
                    Log.e(TAG, "handleMessage: unknownItem:[" + item + "]");
                }
            }

            applicationInfoList.forEach((applicationInfo -> {
                if (!appCfg.containsKey(applicationInfo.packageName))
                    appCfg.put(applicationInfo.packageName, 0);
            }));

            // [-2]:杀死 [-1]:SIGSTOP [0]:freezer [1]:动态 [2]:自由 [3]:内置
            applicationInfoListSort = new ArrayList<>();
            for (int i = 2; i >= -2; i--) {
                for (ApplicationInfo info : applicationInfoList) {
                    Integer mode = appCfg.get(info.packageName);
                    if (mode != null && mode.equals(i)) {
                        applicationInfoListSort.add(info);
                    }
                }
            }
            for (ApplicationInfo info : applicationInfoList) {
                Integer mode = appCfg.get(info.packageName);
                if (mode != null && mode.equals(3)) {
                    applicationInfoListSort.add(info);
                }
            }

            recycleAdapter = new AppCfgAdapter(requireContext(), applicationInfoListSort, appCfg);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            layoutManager.setOrientation(RecyclerView.VERTICAL);
            recyclerView.setAdapter(recycleAdapter);
            recyclerView.setItemAnimator(new DefaultItemAnimator());

            swipeRefreshLayout.setRefreshing(false);
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