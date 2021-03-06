package com.jark006.freezeit;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
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
import com.jark006.freezeit.databinding.FragmentLogcatBinding;

import java.nio.charset.StandardCharsets;

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
                }else if (id == R.id.refresh_log) {
                    new Thread(getLogTask).start();
                }
                return true;
            }
        }, this.getViewLifecycleOwner());

        new Thread(getLogTask).start();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
            if(new String(response, StandardCharsets.UTF_8).startsWith("Unknown")){
                Snackbar.make(constraintLayout,"清空功能在下个模块版本更新", Snackbar.LENGTH_SHORT).show();
                return;
            }
            logView.setMovementMethod(ScrollingMovementMethod.getInstance());//流畅滑动
            logView.setText(new String(response, StandardCharsets.UTF_8));
            forBottom.requestFocus();//请求焦点，直接到日志底部
        }
    };

    Runnable getLogTask = () -> Utils.freezeitTask(Utils.getLog, null, handler);
    Runnable clearLogTask = () -> Utils.freezeitTask(Utils.clearLog, null, handler);

}