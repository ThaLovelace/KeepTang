package com.example.keeptang; // (Package "Root" (ราก) ของคุณ)

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.keeptang.databinding.ActivityMainBinding; // (นี่คือ "ViewBinding" ... "เร็วกว่า" (Faster) 'findViewById')
import com.example.keeptang.ui.calendar.CalendarFragment; // (Import "แท็บ 2")
import com.example.keeptang.ui.charts.ChartsFragment; // (Import "แท็บ 4")
import com.example.keeptang.ui.home.HomeFragment; // (Import "แท็บ 1")
import com.example.keeptang.ui.input.AddTransactionFragment; // (Import "แท็บ 3")
import com.example.keeptang.ui.more.MoreFragment; // (Import "แท็บ 5")
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // (นี่คือ "ทางลัด" (Shortcut) ที่ "ดีที่สุด" (Best)
    // ... ที่ "เชื่อม" (Connect) 'activity_main.xml' (โครงเปลือก))
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // "เชื่อม" (Bind) "โครง XML" (Shell XML)
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // "ตั้งค่า" (Setup) "Navi Bar 5 แท็บ" (5-Tab Navi Bar)
        BottomNavigationView navView = findViewById(R.id.nav_view); // (ID นี้... "Template" (Template) "สร้าง" (Create) ให้เราแล้ว)

        // "โหลด" (Load) "หน้าแรก" (Home) (แท็บ 1) เป็น "Default" (Default)
        loadFragment(new HomeFragment());

        // "ติดตั้ง" (Setup) "ตัวดักฟัง" (Listener) ...
        // ... ที่ "ทำงาน" (Run) ... เมื่อ User "คลิก" (Click) "แท็บ" (Tab)
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // (เราต้อง "หา" (Find) "ID" (ID) ของ "เมนู" (Menu) 5 อัน...
            // ... (ที่อยู่ใน `res/menu/bottom_nav_menu.xml`) ...
            // ... (Template อาจจะ "ตั้งชื่อ" (Name) ว่า 'R.id.navigation_home', 'R.id.navigation_dashboard'...))

            // --- (นี่คือ "Logic" (Logic) "สลับหน้า" (Switch Screen)) ---

            if (itemId == R.id.navigation_home) {
                // (ถ้ากด "แท็บ 1: Home")
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_calendar) {
                // (ถ้ากด "แท็บ 2: Calendar")
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.navigation_add) {
                // (ถ้ากด "แท็บ 3: Add")
                selectedFragment = new AddTransactionFragment();
            } else if (itemId == R.id.navigation_charts) {
                // (ถ้ากด "แท็บ 4: Charts")
                selectedFragment = new ChartsFragment();
            } else if (itemId == R.id.navigation_more) {
                // (ถ้ากด "แท็บ 5: More")
                selectedFragment = new MoreFragment();
            }

            // (ถ้า "เลือก" (Select) "หน้า" (Fragment) แล้ว... "โหลด" (Load) มันเลย)
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * "ผู้ช่วย" (Helper) ... ที่ "สลับ" (Swap) "หน้าจอ" (Fragment)
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // "แทนที่" (Replace) "เนื้อหา" (Content) (ใน 'activity_main.xml') ...
        // ... ด้วย "หน้าจอ" (Fragment) ที่ User "เลือก" (Select)
        fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, fragment);
        fragmentTransaction.commit();
    }
}