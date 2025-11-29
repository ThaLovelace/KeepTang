package com.example.keeptang.ui.input;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.keeptang.MainActivity;
import com.example.keeptang.R;
import com.example.keeptang.data.Category;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.logic.SmartCategorizer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionFragment extends Fragment implements ManualPickerFragment.CategorySelectListener {

    // Views
    private EditText etName;
    private EditText etAmount;
    private LinearLayout btnDatePicker;
    private TextView tvSelectedDate;

    // AI Category Button (New UI)
    private LinearLayout btnAiCategoryPicker;
    private TextView tvAiCategoryName;
    private ImageView ivAiCategoryIcon;

    // Toolbar Buttons
    private ImageView iconSave;
    private ImageView iconDelete;
    private TextView tvTitle;
    // private ImageView iconBack; // ปุ่มย้อนกลับ

    private RadioButton togglePaid;
    private RadioButton toggleIncome;
    private View dimOverlay;

    // Logic & Data
    private DatabaseHelper dbHelper;
    private Calendar selectedCalendar;
    private int currentCategoryId = -1;

    // Edit Mode Variables
    private boolean isEditMode = false;
    private int transactionId = -1;

    // Factory Method สำหรับรับข้อมูลเพื่อแก้ไข
    public static AddTransactionFragment newInstance(int id, String name, double price, int categoryId, String timestamp) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle args = new Bundle();
        args.putInt("ID", id);
        args.putString("NAME", name);
        args.putDouble("PRICE", price);
        args.putInt("CAT_ID", categoryId);
        args.putString("TIME", timestamp);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_transaction, container, false);
        dbHelper = new DatabaseHelper(getContext());

        // Bind Views
        etName = view.findViewById(R.id.et_name);
        etAmount = view.findViewById(R.id.et_amount);
        btnDatePicker = view.findViewById(R.id.btn_date_picker);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);

        // Bind New Category Button
        btnAiCategoryPicker = view.findViewById(R.id.btn_ai_category_picker);
        tvAiCategoryName = view.findViewById(R.id.tv_ai_category_name);
        ivAiCategoryIcon = view.findViewById(R.id.iv_ai_category_icon);

        iconSave = view.findViewById(R.id.icon_save);
        iconDelete = view.findViewById(R.id.icon_delete);
       // iconBack = view.findViewById(R.id.icon_back);
        tvTitle = view.findViewById(R.id.tv_title);

        togglePaid = view.findViewById(R.id.toggle_paid);
        toggleIncome = view.findViewById(R.id.toggle_income);
        dimOverlay = view.findViewById(R.id.dim_background_overlay);

        selectedCalendar = Calendar.getInstance();

        // Setup Mode (Add vs Edit)
        if (getArguments() != null) {
            setupEditMode();
        } else {
            updateCategoryButtonUI(-1); // Default Add Mode
        }

        updateDateLabel();
        setupDatePicker();
        setupAiSearch(); // AI แบบกด Enter
        setupCategoryPickerButton();
        setupSaveButton();
        setupDeleteButton();
      //  setupBackButton();

        return view;
    }

    private void setupEditMode() {
        isEditMode = true;
        transactionId = getArguments().getInt("ID");
        String name = getArguments().getString("NAME");
        double price = getArguments().getDouble("PRICE");
        currentCategoryId = getArguments().getInt("CAT_ID");
        String time = getArguments().getString("TIME");

        tvTitle.setText("Edit");
        iconDelete.setVisibility(View.VISIBLE);

        etName.setText(name);
        etAmount.setText(String.valueOf(Math.abs(price))); // โชว์เลขบวกเสมอ

        if (price >= 0) toggleIncome.setChecked(true);
        else togglePaid.setChecked(true);

        updateCategoryButtonUI(currentCategoryId);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            selectedCalendar.setTime(sdf.parse(time));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupDatePicker() {
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

    // ✅ AI Search: ทำงานเมื่อกดปุ่ม Done/Enter ที่คีย์บอร์ด
    private void setupAiSearch() {
        etName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String itemName = etName.getText().toString().trim();
                if (!itemName.isEmpty()) {
                    // เรียก Smart AI
                    SmartCategorizer.predictCategory(getContext(), itemName, categoryId -> {
                        if (isAdded() && getActivity() != null) {
                            currentCategoryId = categoryId;
                            updateCategoryButtonUI(currentCategoryId);
                        }
                    });
                    // ซ่อนคีย์บอร์ด
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etName.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    // ✅ ปุ่มเลือกหมวดหมู่: ส่งโหมด Paid/Income ไปด้วย
    private void setupCategoryPickerButton() {
        btnAiCategoryPicker.setOnClickListener(v -> {
            boolean isExpense = togglePaid.isChecked();
            ManualPickerFragment pickerFragment = ManualPickerFragment.newInstance(isExpense);

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
            if (togglePaid.isChecked()) amount = -Math.abs(amount); // จ่าย = ติดลบ
            else amount = Math.abs(amount); // รับ = บวก

            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = dbFormat.format(selectedCalendar.getTime());

            if (isEditMode) {
                // Update
                dbHelper.updateTransaction(transactionId, name, amount, currentCategoryId, timestamp);
                Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();
            } else {
                // Insert
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COL_TR_NAME, name);
                values.put(DatabaseHelper.COL_TR_PRICE, amount);
                values.put(DatabaseHelper.COL_TR_CATEGORY_ID, currentCategoryId);
                values.put(DatabaseHelper.COL_TR_TIMESTAMP, timestamp);
                db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);

                // สอน AI (เฉพาะตอนเพิ่มใหม่)
                dbHelper.teachAI(name, currentCategoryId);
                Toast.makeText(getContext(), R.string.toast_saved, Toast.LENGTH_SHORT).show();
            }

            exitPage();
        });
    }

    private void setupDeleteButton() {
        iconDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteTransaction(transactionId);
                        Toast.makeText(getContext(), "Deleted!", Toast.LENGTH_SHORT).show();
                        exitPage();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

   /* private void setupBackButton() {
        iconBack.setOnClickListener(v -> exitPage());
    } */

    private void exitPage() {
        resetInputForm();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToHome();
        }
    }

    private void resetInputForm() {
        etName.setText("");
        etAmount.setText("");
        currentCategoryId = -1;
        updateCategoryButtonUI(-1);
        selectedCalendar = Calendar.getInstance();
        updateDateLabel();
        togglePaid.setChecked(true);
        isEditMode = false;
        iconDelete.setVisibility(View.GONE);
        if (tvTitle != null) tvTitle.setText("Input");
    }

    // Callbacks from Popup
    @Override
    public void onCategorySelected(Category category) {
        currentCategoryId = category.getId();
        updateCategoryButtonUI(currentCategoryId);

        // สอน AI เมื่อเลือกเอง
        String currentName = etName.getText().toString().trim();
        if (!currentName.isEmpty()) {
            dbHelper.teachAI(currentName, currentCategoryId);
        }
    }

    @Override
    public void onPopupDismissed() {
        dimOverlay.setVisibility(View.GONE);
    }

    @SuppressLint("DiscouragedApi")
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
            String categoryName = cursor.getString(0);
            String iconName = cursor.getString(1);
            int iconResId = 0;
            if (getContext() != null) {
                iconResId = getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            }
            tvAiCategoryName.setText(categoryName);
            ivAiCategoryIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.icon_edit);
        } else {
            // กรณีหา ID ไม่เจอ (เช่น ลบหมวดหมู่ไปแล้ว)
            tvAiCategoryName.setText(R.string.picker_not_selected);
            ivAiCategoryIcon.setImageResource(R.drawable.icon_edit);
        }
        cursor.close();
    }
}