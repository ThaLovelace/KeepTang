package com.example.keeptang.ui.input;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.data.Category;
import com.example.keeptang.data.DatabaseHelper;
import com.example.keeptang.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ManualPickerFragment extends BottomSheetDialogFragment implements CategoryPickerAdapter.OnCategoryClickListener {

    public interface CategorySelectListener {
        void onCategorySelected(Category category);
        void onPopupDismissed();
    }

    private CategorySelectListener mListener;
    private RecyclerView recyclerView;
    private CategoryPickerAdapter adapter;
    private List<Category> categoryList;
    private DatabaseHelper dbHelper;

    // ตัวแปรเช็คว่าเป็นรายจ่ายหรือไม่
    private boolean isExpenseMode = true;

    // ✅ ฟังก์ชันสำหรับส่งค่า "โหมด" เข้ามา
    public static ManualPickerFragment newInstance(boolean isExpense) {
        ManualPickerFragment fragment = new ManualPickerFragment();
        Bundle args = new Bundle();
        args.putBoolean("IS_EXPENSE", isExpense);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isExpenseMode = getArguments().getBoolean("IS_EXPENSE", true);
        }
    }

    // ... (onAttach เหมือนเดิม) ...
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof CategorySelectListener) {
            mListener = (CategorySelectListener) getTargetFragment();
        } else if (getParentFragment() instanceof CategorySelectListener) {
            mListener = (CategorySelectListener) getParentFragment();
        } else if (context instanceof CategorySelectListener) {
            mListener = (CategorySelectListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manual_picker, container, false);

        dbHelper = new DatabaseHelper(getContext());
        recyclerView = view.findViewById(R.id.recycler_category_picker);

        loadCategoriesFromDatabase(); // โหลดตามโหมด

        adapter = new CategoryPickerAdapter(getContext(), categoryList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void loadCategoriesFromDatabase() {
        categoryList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // ✅ กรองข้อมูลตาม ID (ที่เรา Hardcode ไว้ใน DatabaseHelper)
        // รายจ่าย = ID 1-9, 14
        // รายรับ = ID 10-13
        String selection;
        if (isExpenseMode) {
            selection = DatabaseHelper.COL_CAT_ID + " IN (1,2,3,4,5,6,7,8,9,14)";
        } else {
            selection = DatabaseHelper.COL_CAT_ID + " IN (10,11,12,13)";
        }

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CATEGORIES,
                null,
                selection,
                null,
                null, null, null
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String iconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));
            categoryList.add(new Category(id, name, iconName));
        }
        cursor.close();

        // ปุ่ม Add เพิ่มเฉพาะตอนเป็นรายจ่าย (หรือจะใส่ทั้งคู่ก็ได้)
        if (isExpenseMode) {
            categoryList.add(new Category(-1, "+", "icon_add_plus"));
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onPopupDismissed();
        }
    }

    @Override
    public void onCategoryClick(Category category) {
        if (category.getId() != -1) {
            if (mListener != null) {
                mListener.onCategorySelected(category);
            }
            dismiss();
        }
    }
}