package com.jark006.freezeit;


import android.annotation.SuppressLint;
import android.os.Build;
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

import com.jark006.freezeit.databinding.FragmentLogcatBinding;

import java.nio.charset.StandardCharsets;
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
//                      佛祖坐镇 尔等bug小怪速速离去

public class LogcatFragment extends Fragment {
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
                    new Thread(clearLogTask).start();
//                    test();
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        return binding.getRoot();
    }

    void test() {
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null && manufacturer.length() > 0) {
            String phone_type = manufacturer.toLowerCase();
            Log.i("manufacturer", "initView: " + phone_type);

        }

    }

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
                new Thread(getLogTask).start();
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
                logView.setText("");
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

    Runnable getLogTask = () -> Utils.freezeitTask(Utils.getLog, null, handler);
    Runnable clearLogTask = () -> Utils.freezeitTask(Utils.clearLog, null, handler);

}