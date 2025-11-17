package com.example.keeptang;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.keeptang.databinding.ActivityMainBinding;
import com.example.keeptang.ui.calendar.CalendarFragment;
import com.example.keeptang.ui.charts.ChartsFragment;
import com.example.keeptang.ui.home.HomeFragment;
import com.example.keeptang.ui.input.AddTransactionFragment;
import com.example.keeptang.ui.more.MoreFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. เชื่อมต่อ Layout
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. อ้างอิง BottomNavigationView
        BottomNavigationView navView = binding.navView;

        // ✅✅✅ แก้ไขจุดที่ 1: สั่งให้แสดง "สีจริง" ของไอคอน (ห้ามย้อมสี) ✅✅✅
        navView.setItemIconTintList(null);

        // 3. โหลดหน้าแรก
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // 4. ตัวดักฟังปุ่มเมนู
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.navigation_add) {
                selectedFragment = new AddTransactionFragment();
            } else if (itemId == R.id.navigation_charts) {
                selectedFragment = new ChartsFragment();
            } else if (itemId == R.id.navigation_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, fragment);
        fragmentTransaction.commit();
    }

    // ✅✅✅ ฟังก์ชันสำหรับให้หน้าอื่นสั่งย้ายกลับมาหน้า Home ✅✅✅
    public void switchToHome() {
        // สั่งให้ Navbar เลือกกดปุ่ม Home (มันจะไปทำงานใน Listener และโหลดหน้า Home ให้เอง)
        binding.navView.setSelectedItemId(R.id.navigation_home);
    }
}