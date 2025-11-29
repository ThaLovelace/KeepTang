package com.example.keeptang.ui.more;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout; // ✅ Import อันนี้แทน TextView
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.keeptang.R;
import com.example.keeptang.logic.Gamification;

public class MoreFragment extends Fragment {

    private TextView tvPoints;
    private LinearLayout btnPointsContainer;
    // ✅ แก้ไข: เปลี่ยนจาก TextView เป็น RelativeLayout
    private RelativeLayout btnMenuStore;
    private RelativeLayout btnMenuCategories;
    private RelativeLayout btnMenuBank;

    private Gamification gamificationLogic;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        if (getContext() != null) {
            gamificationLogic = new Gamification(getContext());
        }

        tvPoints = view.findViewById(R.id.tv_points);
        btnPointsContainer = view.findViewById(R.id.btn_points_container);

        // ✅ แก้ไข: เชื่อมต่อกับ RelativeLayout ใน XML
        btnMenuStore = view.findViewById(R.id.btn_menu_store);
        btnMenuCategories = view.findViewById(R.id.btn_menu_categories);
        btnMenuBank = view.findViewById(R.id.btn_menu_bank);

        setupDisabledButtons();
        setupPointsClickListener();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null && gamificationLogic != null) {
            // ✅ แก้ไข: เรียกใช้ getTotalPoints (ต้องมั่นใจว่าใน Gamification.java มีเมธอดนี้แล้ว)
            // ถ้ายังไม่มี ให้ใช้ performDailyCheckIn() แก้ขัดไปก่อน หรือไปเพิ่มเมธอดใน Gamification.java
            int currentPoints = gamificationLogic.getTotalPoints();
            tvPoints.setText(String.valueOf(currentPoints));
        }
    }

    private void setupPointsClickListener() {
        btnPointsContainer.setOnClickListener(v -> {
            Gamification.CheckInResult checkInResult = gamificationLogic.performDailyCheckIn();
            showCheckInDialog(checkInResult.pointsEarned, checkInResult.streak);
        });
    }

    private void showCheckInDialog(int pointsEarned, int streak) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_checkin, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogPoints = dialogView.findViewById(R.id.tv_checkin_points);
        Button btnCollect = dialogView.findViewById(R.id.btn_collect);

        tvDialogPoints.setText("+" + pointsEarned + " Points");

        for (int i = 1; i <= 7; i++) {
            int resId = getResources().getIdentifier("day_" + i, "id", getContext().getPackageName());
            View dayView = dialogView.findViewById(resId);

            if (dayView != null) {
                TextView tvDayPoints = dayView.findViewById(R.id.tv_day_points);
                ImageView ivIcon = dayView.findViewById(R.id.iv_day_icon);

                int dayPoints = gamificationLogic.getPointsForDay(i);
                tvDayPoints.setText("+" + dayPoints);

                if (i == 5) ivIcon.setImageResource(R.drawable.icon_10point);
                else if (i == 7) ivIcon.setImageResource(R.drawable.icon_15point);
                else ivIcon.setImageResource(R.drawable.icon_5point);

                if (i < streak) {
                    dayView.setAlpha(0.5f);
                    ivIcon.setImageResource(R.drawable.icon_check);
                } else if (i == streak) {
                    dayView.setBackgroundResource(R.drawable.button_selector_yellow);
                    dayView.setAlpha(1.0f);
                } else {
                    dayView.setBackgroundResource(R.drawable.rounded_edittext_white);
                    dayView.setAlpha(1.0f);
                }
            }
        }

        btnCollect.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupDisabledButtons() {
        View.OnClickListener comingSoonListener = v -> {
            if (getContext() != null) {
                // ✅ แก้ไข: เรียกใช้ R.string.toast_coming_soon (ต้องมีใน strings.xml)
                Toast.makeText(getContext(), R.string.toast_coming_soon, Toast.LENGTH_SHORT).show();
            }
        };

        btnMenuStore.setOnClickListener(comingSoonListener);
        btnMenuCategories.setOnClickListener(comingSoonListener);
        btnMenuBank.setOnClickListener(comingSoonListener);
    }
}
