package com.example.keeptang.ui.input;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.keeptang.R;
import com.example.keeptang.data.Category;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.logic.AutoCategorizer;

// (Implement OnCancelListener เพื่อ "ดักฟัง" (Listen) ตอน "ลาก" (Drag) Popup ปิด)
public class AddTransactionFragment extends Fragment implements ManualPickerFragment.CategorySelectListener, DialogInterface.OnCancelListener {

    private EditText etName;
    private EditText etAmount;
    private Button btnAiCategoryPicker;
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

        etName = view.findViewById(R.id.et_name);
        etAmount = view.findViewById(R.id.et_amount);
        btnAiCategoryPicker = view.findViewById(R.id.btn_ai_category_picker);
        iconSave = view.findViewById(R.id.icon_save);
        togglePaid = view.findViewById(R.id.toggle_paid);
        dimOverlay = view.findViewById(R.id.dim_background_overlay);

        setupAiTextWatcher();
        setupCategoryPickerButton();
        setupSaveButton();

        return view;
    }

    private void setupAiTextWatcher() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String itemName = s.toString();
                // "ปรับโค้ด" (Optimize) (แก้ Warning "redundant")
                currentCategoryId = AutoCategorizer.guessCategory(itemName);
                updateCategoryButtonUI(currentCategoryId);
            }
        });
    }

    private void setupCategoryPickerButton() {
        btnAiCategoryPicker.setOnClickListener(v -> {
            ManualPickerFragment pickerFragment = new ManualPickerFragment();

            // "เปลี่ยน" (Change) 'setParentFragment' (ที่ "พัง" (Error))
            // เป็น "setTargetFragment"
            pickerFragment.setTargetFragment(this, 123); // (Request Code 123)

            dimOverlay.setVisibility(View.VISIBLE);
            pickerFragment.show(getParentFragmentManager(), "CategoryPicker");
        });
    }

    private void setupSaveButton() {
        iconSave.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String amountStr = etAmount.getText().toString();

            // (ตอนนี้... 'error_fill_all' จะ "หาเจอ" (Resolved) ...
            // ... เพราะเรา "เพิ่ม" (Add) มันใน 'strings.xml' แล้ว)
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

            // ✅✅✅ "Bug Fix" #2: "แก้ไข" (FIX) "Typo" (Typo) ✅✅✅
            // (เปลี่ยน 'DatabaseLProvider' เป็น 'DatabaseHelper')
            values.put(DatabaseHelper.COL_TR_PRICE, amount);

            values.put(DatabaseHelper.COL_TR_CATEGORY_ID, currentCategoryId);

            long newRowId = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);

            if (newRowId != -1) {
                Toast.makeText(getContext(), R.string.toast_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.toast_error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onCategorySelected(Category category) {
        currentCategoryId = category.getId();
        updateCategoryButtonUI(currentCategoryId);
        dimOverlay.setVisibility(View.GONE);
    }

    // "ลบ" (Remove) "@Override" ... (นี่คือ "Implement" (Implement) ... ไม่ใช่ "Override" (Override))
    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        // super.onCancel(dialog); // (Fragment "ไม่" (Doesn't) มี 'super.onCancel')
        dimOverlay.setVisibility(View.GONE);
    }


    @SuppressLint("DiscouragedApi")
    private void updateCategoryButtonUI(int categoryId) {
        // (เรา "ต้อง" (Must) "เช็ก" (Check) 'getContext' ...
        // ... เพื่อ "ป้องกัน" (Prevent) "NullPointerException" (Warning))
        if (getContext() == null) {
            return;
        }

        if (categoryId == -1) {
            btnAiCategoryPicker.setText(R.string.picker_not_selected);
            // "เปลี่ยน" (Change) "ไอคอน" (Icon) ที่ "พัง" (Error) ... เป็น "ของจริง" (Real)
            btnAiCategoryPicker.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_edit, 0);
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
            // "ป้องกัน" (Prevent) "NullPointerException" (Warning)
            if (getContext() != null) {
                iconResId = getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
            }

            btnAiCategoryPicker.setText(categoryName);
            // "เปลี่ยน" (Change) "ไอคอน" (Icon) ที่ "พัง" (Error) ... เป็น "ของจริง" (Real)
            btnAiCategoryPicker.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, R.drawable.icon_edit, 0);
        }
        cursor.close();
    }
}