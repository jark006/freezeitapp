package com.jark006.freezeit;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.jark006.freezeit.databinding.FragmentConfigBinding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
//                      佛祖坐镇 尔等bug小怪速速离去

public class ConfigFragment extends Fragment implements Handler.Callback {

    private FragmentConfigBinding binding;
    appListRecyclerAdapter recycleAdapter;
    List<ApplicationInfo> applicationInfoList = new ArrayList<>();
    List<ApplicationInfo> applicationInfoListSort = new ArrayList<>();
    HashSet<String> whiteListForce = new HashSet<>();
    HashSet<String> whiteListConf = new HashSet<>();
    byte[] newConf;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigBinding.inflate(inflater, container, false);

        recyclerView = binding.recyclerviewApp;
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(getWhitelistTask).start());

        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> aLLApplicationInfoList = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : aLLApplicationInfoList) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
                continue;
            applicationInfoList.add(appInfo);
        }
        new Thread(getWhitelistTask).start();

        return binding.getRoot();
    }

    private void refreshListView() {

        applicationInfoListSort.clear();
        for (ApplicationInfo info : applicationInfoList) {
            if (whiteListForce.contains(info.packageName)) {
                applicationInfoListSort.add(info);
            }
        }
        for (ApplicationInfo info : applicationInfoList) {
            if (whiteListConf.contains(info.packageName)) {
                applicationInfoListSort.add(info);
            }
        }

        for (ApplicationInfo info : applicationInfoList) {
            if (!whiteListForce.contains(info.packageName) && !whiteListConf.contains(info.packageName)) {
                applicationInfoListSort.add(info);
            }
        }

        recycleAdapter = new appListRecyclerAdapter(requireContext(),
                applicationInfoListSort, whiteListForce, whiteListConf, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setAdapter(recycleAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private final Handler getWhitelistHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                return;
            }

            whiteListForce.clear();
            whiteListConf.clear();

            String[] towList = new String(response, StandardCharsets.UTF_8).split("####");
            int len = towList.length;

            if (len > 0) {
                String[] whitelistStr = towList[0].split("\n");
                whiteListForce.addAll(Arrays.asList(whitelistStr));
                if (len > 1) {
                    whitelistStr = towList[1].split("\n");
                    whiteListConf.addAll(Arrays.asList(whitelistStr));
                }
            }

            refreshListView();
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    Runnable getWhitelistTask = () -> Utils.freezeitTask(Utils.getWhiteList, null, getWhitelistHandler);


    private final Handler setWhitelistHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Toast.makeText(getContext(), R.string.update_seccess, Toast.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]";
                Toast.makeText(getContext(), errorTips, Toast.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
            }
        }
    };
    Runnable setWhitelistTask = () -> Utils.freezeitTask(Utils.setWhiteList, newConf, setWhitelistHandler);


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        String response = msg.getData().getString("response");
        int position = msg.getData().getInt("position");

        if (response == null || response.length() == 0) {
            newConf = null;
        } else {
            Log.i(TAG, "handleMessage: " + response);
            String[] newConfStr = response.split("\n");
            whiteListConf.clear();
            whiteListConf.addAll(Arrays.asList(newConfStr));
            newConf = response.getBytes(StandardCharsets.UTF_8);
        }
        recycleAdapter.notifyItemChanged(position, 0);

        new Thread(setWhitelistTask).start();

        return true;
    }
}