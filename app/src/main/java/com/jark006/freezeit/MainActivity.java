package com.jark006.freezeit;

import static androidx.constraintlayout.widget.Constraints.TAG;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.jark006.freezeit.databinding.ActivityMainBinding;

import java.nio.charset.StandardCharsets;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_config, R.id.navigation_logcat)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        this.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.config_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.update_app_name) {
                    Toast.makeText(getApplicationContext(), getString(R.string.update_start), Toast.LENGTH_SHORT).show();
                    new Thread(updateAppNameTask).start();
                }else if (id == R.id.about) {
                    aboutDialog();
                }
                return false;
            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            byte[] response = msg.getData().getByteArray("response");

            if (response == null || response.length == 0) {
                Toast.makeText(getBaseContext(), getString(R.string.freezeit_offline), Toast.LENGTH_SHORT).show();
                return;
            }

            String res = new String(response, StandardCharsets.UTF_8);
            if (res.equals("success")) {
                Toast.makeText(getApplicationContext(), R.string.update_seccess, Toast.LENGTH_SHORT).show();
            } else {
                String errorTips = getString(R.string.update_fail)+" Receive:[" + res + "]";
                Toast.makeText(getApplicationContext(), errorTips, Toast.LENGTH_SHORT).show();
                Log.e(TAG, errorTips);
            }
        }
    };

    Runnable updateAppNameTask = () -> {
        StringBuilder appName = new StringBuilder();

        PackageManager pm = getBaseContext().getPackageManager();
        List<ApplicationInfo> applicationsInfo = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : applicationsInfo) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0)
                continue;

            String label = pm.getApplicationLabel(appInfo).toString();
            if (label.endsWith("Application") || label.endsWith(".xml") || label.endsWith("false"))
                label = appInfo.packageName;

            appName.append(appInfo.packageName).append("####").append(label).append('\n');
        }
        Utils.freezeitTask(Utils.setAppName, appName.toString().getBytes(StandardCharsets.UTF_8), handler);
    };

    public void aboutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.about);
        dialog.show();
    }
}