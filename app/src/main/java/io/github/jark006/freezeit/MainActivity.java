package io.github.jark006.freezeit;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.github.jark006.freezeit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPrivacy(this);

        StaticData.am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_config, R.id.navigation_logcat)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        new Thread(() -> AppInfoCache.refreshCache(this)).start();

        try {
            StaticData.bg = Drawable.createFromPath(
                    this.getFilesDir().getPath() + "/" + StaticData.bgFileName);
            if (StaticData.bg != null)
                StaticData.bg.setAlpha(56);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        findViewById(R.id.container).setBackground(StaticData.getBackgroundDrawable(this));
    }

    public static void checkPrivacy(Context context) {
        SharedPreferences sf = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        final String key = BuildConfig.VERSION_NAME + "isAccept";
        var isAccept = sf.getBoolean(key, false);
        if (isAccept) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.privacy_title).setMessage(R.string.privacy_content)
                .setNegativeButton(R.string.reject, (dialog, which) -> System.exit(0))
                .setPositiveButton(R.string.accept, (dialog, which) -> {
                    var edit = sf.edit();
                    edit.putBoolean(key, true);
                    edit.apply();
                })
                .setCancelable(false)
                .create()
                .show();
    }

}