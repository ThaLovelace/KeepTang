package com.example.keeptang.ui.input;

import android.content.Context;
import android.content.DialogInterface; // ✅ Import เพิ่ม
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
        void onPopupDismissed(); // ✅ เพิ่มบรรทัดนี้: เพื่อสั่งหน้าแม่ให้ซ่อนพื้นหลังดำ
    }

    private CategorySelectListener mListener;

    private RecyclerView recyclerView;
    private CategoryPickerAdapter adapter;
    private List<Category> categoryList;
    private DatabaseHelper dbHelper;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // โค้ดส่วนนี้ถูกต้องแล้ว (รองรับทั้ง getTargetFragment, getParentFragment, context)
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

        loadCategoriesFromDatabase();

        adapter = new CategoryPickerAdapter(getContext(), categoryList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void loadCategoriesFromDatabase() {
        categoryList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = DatabaseHelper.COL_CAT_NAME + " != ?";
        String[] selectionArgs = {"รายรับ"}; // หรือ "Income" ถ้าฐานข้อมูลเป็นภาษาอังกฤษ

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CATEGORIES,
                null,
                selection,
                selectionArgs,
                null, null, null
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String iconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            categoryList.add(new Category(id, name, iconName));
        }
        cursor.close();

        categoryList.add(new Category(-1, "+", "icon_add_plus"));
    }

    // ✅✅✅ เพิ่มเมธอดนี้: ทำงานเมื่อ Popup ถูกปิด (ไม่ว่าจะด้วยวิธีไหน)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onPopupDismissed(); // บอกหน้าแม่ให้เอาพื้นหลังสีดำออก
        }
    }

    @Override
    public void onCategoryClick(Category category) {
        if (category.getId() == -1) {
            // ปุ่ม + ยังไม่ทำอะไร
        } else {
            if (mListener != null) {
                mListener.onCategorySelected(category);
            }
            dismiss(); // สั่งปิด Popup (มันจะไปเรียก onDismiss ทำงานต่อเอง)
        }
    }
}