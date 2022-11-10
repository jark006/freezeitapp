package com.jark006.freezeit.fragment;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.jark006.freezeit.R;
import com.jark006.freezeit.Utils;
import com.jark006.freezeit.databinding.FragmentLogcatBinding;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

public class LogcatFragment extends Fragment {
    private final static String TAG = "LogcatFragment";
    private FragmentLogcatBinding binding;
    ConstraintLayout constraintLayout;
    TextView logView;
    LinearLayout forBottom;

    Timer timer;

    int lastLogLen = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogcatBinding.inflate(inflater, container, false);

        constraintLayout = binding.constraintLayoutLogcat;
        logView = binding.logView;
        forBottom = binding.forBottom;

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.logcat_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.clear_log) {
                    new Thread(() -> Utils.freezeitTask(Utils.clearLog, null, handler)).start();
                } else if (id == R.id.printf_freeze) {
                    new Thread(() -> Utils.freezeitTask(Utils.printFreezerProc, null, handler)).start();
                } else if (id == R.id.help_log) {
                    Utils.imgDialog(requireContext(), R.drawable.help_logcat);
                } else if (id == R.id.update_label) {
                    Snackbar.make(constraintLayout, getString(R.string.update_start), Snackbar.LENGTH_SHORT).show();
                    new Thread(updateAppLabelTask).start();
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        return binding.getRoot();
    }

    private final Handler appNameHandler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                String errorTips = getString(R.string.freezeit_offline);
                Snackbar.make(constraintLayout, errorTips, Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
                return;
            }

            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Snackbar.make(constraintLayout, R.string.update_success, Snackbar.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail) + " Receive:[" + res + "]";
                Snackbar.make(constraintLayout, errorTips, Snackbar.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
            }
        }
    };

    Runnable updateAppLabelTask = () -> {
        StringBuilder appName = new StringBuilder();

        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> applicationsInfo = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : applicationsInfo) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
                continue;
            if (appInfo.uid < 10000)
                continue;

            String label = pm.getApplicationLabel(appInfo).toString();
            appName.append(appInfo.packageName).append("####").append(label).append('\n');
        }
        Utils.freezeitTask(Utils.setAppLabel, appName.toString().getBytes(StandardCharsets.UTF_8), appNameHandler);
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> Utils.freezeitTask(Utils.getLog, null, handler)).start();
            }
        }, 0, 3000);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                logView.setText("null");
                return;
            }

            if (lastLogLen == response.length)
                return;

            lastLogLen = response.length;

            logView.setMovementMethod(ScrollingMovementMethod.getInstance());//流畅滑动
            logView.setText(new String(response, StandardCharsets.UTF_8));
            forBottom.requestFocus();//请求焦点，直接到日志底部
            forBottom.clearFocus();
        }
    };
}