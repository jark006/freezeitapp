package com.jark006.freezeit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
    TextView logView;
    LinearLayout forBottom;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogcatBinding.inflate(inflater, container, false);

        logView = binding.logView;
        forBottom = binding.forBottom;

        new Thread(networkTask).start();

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
                return;
            }

            logView.setMovementMethod(ScrollingMovementMethod.getInstance());//流畅滑动
            logView.setText(new String(response, StandardCharsets.UTF_8));
            forBottom.requestFocus();//请求焦点，直接到日志底部
        }
    };

    Runnable networkTask = () -> Utils.freezeitTask(Utils.getlog, null, handler);

}