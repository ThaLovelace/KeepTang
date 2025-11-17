package com.example.keeptang.ui.input;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // (ไม่ได้ใช้แล้ว แต่ import ไว้ไม่เป็นไร)
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.keeptang.MainActivity; // ✅ Import เพื่อเรียกใช้ switchToHome
import com.example.keeptang.R;
import com.example.keeptang.data.Category;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.logic.AutoCategorizer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionFragment extends Fragment implements ManualPickerFragment.CategorySelectListener {
    // (ลบ OnCancelListener ออกจากหัวบรรทัด เพราะเราใช้ onPopupDismissed ใน ManualPickerFragment แทนแล้ว)

    private EditText etName;
    private EditText etAmount;

    // ตัวแปรสำหรับ Date Picker
    private LinearLayout btnDatePicker;
    private TextView tvSelectedDate;
    private Calendar selectedCalendar;

    private LinearLayout btnAiCategoryPicker;
    private TextView tvAiCategoryName;
    private ImageView ivAiCategoryIcon;

    private ImageView iconSave;
    private RadioButton togglePaid;
    private View dimOverlay;

    private DatabaseHelper dbHelper;
    private int currentCategoryId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_transaction, container, false);
        dbHelper = new DatabaseHelper(getContext());

        // เชื่อม View
        etName = view.findViewById(R.id.et_name);
        etAmount = view.findViewById(R.id.et_amount);

        btnDatePicker = view.findViewById(R.id.btn_date_picker);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);

        btnAiCategoryPicker = view.findViewById(R.id.btn_ai_category_picker);
        tvAiCategoryName = view.findViewById(R.id.tv_ai_category_name);
        ivAiCategoryIcon = view.findViewById(R.id.iv_ai_category_icon);

        iconSave = view.findViewById(R.id.icon_save);
        togglePaid = view.findViewById(R.id.toggle_paid);
        dimOverlay = view.findViewById(R.id.dim_background_overlay);

        setupDatePicker();
        setupAiTextWatcher();
        setupCategoryPickerButton();
        setupSaveButton();

        return view;
    }

    private void setupDatePicker() {
        selectedCalendar = Calendar.getInstance();
        updateDateLabel();

        btnDatePicker.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateLabel();
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void updateDateLabel() {
        String myFormat = "dd MMMM yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        tvSelectedDate.setText(sdf.format(selectedCalendar.getTime()));
    }

    private void setupAiTextWatcher() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String itemName = s.toString();
                currentCategoryId = AutoCategorizer.guessCategory(itemName);
                updateCategoryButtonUI(currentCategoryId);
            }
        });
    }

    private void setupCategoryPickerButton() {
        btnAiCategoryPicker.setOnClickListener(v -> {
            ManualPickerFragment pickerFragment = new ManualPickerFragment();
            // ใช้ setTargetFragment แบบเดิม (เพราะเราแก้ onAttach ให้รองรับแล้ว) หรือใช้ setParentFragment ก็ได้
            // แต่เพื่อให้ชัวร์กับ ManualPickerFragment ที่แก้ไป ให้ใช้ setTargetFragment (123) เหมือนเดิมปลอดภัยสุด
            pickerFragment.setTargetFragment(this, 123);

            dimOverlay.setVisibility(View.VISIBLE);
            pickerFragment.show(getParentFragmentManager(), "CategoryPicker");
        });
    }

    private void setupSaveButton() {
        iconSave.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String amountStr = etAmount.getText().toString();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(amountStr) || currentCategoryId == -1) {
                Toast.makeText(getContext(), R.string.error_fill_all, Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            boolean isExpense = togglePaid.isChecked();

            if (isExpense) {
                amount = -Math.abs(amount);
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.COL_TR_NAME, name);
            values.put(DatabaseHelper.COL_TR_PRICE, amount);
            values.put(DatabaseHelper.COL_TR_CATEGORY_ID, currentCategoryId);

            // บันทึกวันที่
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = dbFormat.format(selectedCalendar.getTime());
            values.put(DatabaseHelper.COL_TR_TIMESTAMP, timestamp);

            long newRowId = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);

            if (newRowId != -1) {
                Toast.makeText(getContext(), R.string.toast_saved, Toast.LENGTH_SHORT).show();

                // ✅ 1. เคลียร์ค่า
                resetInputForm();

                // ✅ 2. สั่งให้ MainActivity เด้งไปหน้า Home
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToHome();
                }

            } else {
                Toast.makeText(getContext(), R.string.toast_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetInputForm() {
        etName.setText("");
        etAmount.setText("");
        currentCategoryId = -1;
        updateCategoryButtonUI(-1);
        // รีเซ็ตวันที่เป็นปัจจุบัน
        selectedCalendar = Calendar.getInstance();
        updateDateLabel();
        // รีเซ็ต RadioButton เป็น Paid (ถ้าต้องการ)
        togglePaid.setChecked(true);
    }

    @Override
    public void onCategorySelected(Category category) {
        currentCategoryId = category.getId();
        updateCategoryButtonUI(currentCategoryId);
        // dimOverlay จะถูกซ่อนโดย onPopupDismissed ที่ ManualPickerFragment เรียก
    }

    // ✅ รับคำสั่งจาก Popup เมื่อปิดลง
    @Override
    public void onPopupDismissed() {
        dimOverlay.setVisibility(View.GONE);
    }

    private void updateCategoryButtonUI(int categoryId) {
        if (getContext() == null) return;

        if (categoryId == -1) {
            tvAiCategoryName.setText(R.string.picker_not_selected);
            ivAiCategoryIcon.setImageResource(R.drawable.icon_edit);
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CATEGORIES,
                new String[]{DatabaseHelper.COL_CAT_NAME, DatabaseHelper.COL_CAT_ICON},
                DatabaseHelper.COL_CAT_ID + " = ?",
                new String[]{String.valueOf(categoryId)},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String iconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));
            int iconResId = 0;
            if (getContext() != null) {
                iconResId = getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            }
            tvAiCategoryName.setText(categoryName);
            ivAiCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.icon_edit);
        }
        cursor.close();
    }
}