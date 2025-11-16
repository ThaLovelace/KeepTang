package com.example.keeptang.ui.more; // (Package 'ui.more' ของคุณ)

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.keeptang.R;
import com.example.keeptang.logic.Gamification; // (Import 'logic')

public class MoreFragment extends Fragment {

    // 1. "ประกาศ" (Declare) "View" (UI)
    private TextView tvPoints;
    private TextView btnMenuStore;
    private TextView btnMenuCategories;
    private TextView btnMenuBank;

    // 2. "ประกาศ" (Declare) "Logic" (Logic)
    private Gamification gamificationLogic; // "สมองเกม" (Game Brain)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 3. "โหลด" (Inflate) "โครง XML" (XML Layout) 'fragment_more.xml'
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        // 4. "เชื่อม" (Connect) "Logic" (Logic)
        gamificationLogic = new Gamification(getContext());

        // 5. "เชื่อม" (Connect) "View" (UI)
        tvPoints = view.findViewById(R.id.tv_points_more);
        btnMenuStore = view.findViewById(R.id.btn_menu_store);
        btnMenuCategories = view.findViewById(R.id.btn_menu_categories);
        btnMenuBank = view.findViewById(R.id.btn_menu_bank);

        // 6. "ติดตั้ง" (Setup) "ตัวดักฟัง" (Listeners) "ปุ่มที่ปิดไว้" (Disabled Buttons)
        setupDisabledButtons();

        return view;
    }

    /**
     * "onResume()" (onResume) ...
     * (มันจะ "ทำงาน" (Run) "ทุกครั้ง" (Every time) ที่ User "กลับ" (Come back) มาที่ "แท็บ" (Tab) นี้)
     */
    @Override
    public void onResume() {
        super.onResume();

        // 1. "รัน" (Run) "Logic เช็คอิน" (Check-in Logic)!
        int currentPoints = gamificationLogic.performDailyCheckIn();

        // 2. "อัปเดต" (Update) "แต้ม" (Points) (บน Top Bar)
        tvPoints.setText(String.valueOf(currentPoints));
    }

    /**
     * "ติดตั้ง" (Setup) "ตัวดักฟัง" (Listener) ...
     * ... ที่ "ทำงาน" (Run) ... เมื่อ User "คลิก" (Click) "ปุ่มที่ปิดไว้" (Disabled Buttons)
     */
    private void setupDisabledButtons() {
        // (เราจะ "สร้าง" (Create) String "เร็วๆ นี้" (Coming Soon) ...
        // ... (คุณต้อง "เพิ่ม" (Add) `<string name="toast_coming_soon">Coming Soon!</string>` ใน 'strings.xml' นะคะ))

        View.OnClickListener comingSoonListener = v -> {
            Toast.makeText(getContext(), R.string.toast_coming_soon, Toast.LENGTH_SHORT).show();
        };

        // "ผูก" (Bind) "Listener" (Listener) ... เข้ากับ "ปุ่ม" (Buttons) ทั้ง 3
        btnMenuStore.setOnClickListener(comingSoonListener);
        btnMenuCategories.setOnClickListener(comingSoonListener);
        btnMenuBank.setOnClickListener(comingSoonListener);
    }
}