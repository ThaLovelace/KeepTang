package com.example.keeptang.ui.input; // (Package ที่คุณจัดระเบียบใหม่)

import android.content.Context;
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

import com.example.keeptang.data.Category; // (Import "โมเดล" (Model) จาก 'data')
import com.example.keeptang.data.DatabaseHelper; // (Import "ฐานข้อมูล" (Database) จาก 'data')
import com.example.keeptang.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

// (นี่คือ "ตัว Popup" (Popup)... เรา "Extend" (สืบทอด) BottomSheetDialogFragment)
public class ManualPickerFragment extends BottomSheetDialogFragment implements CategoryPickerAdapter.OnCategoryClickListener {

    // (นี่คือ "Interface" (ตัวสื่อสาร) ...
    // ...เพื่อให้ "Popup" (Popup) "ส่ง" (Send) "Category ที่ User เลือก" (Selected Category) ...
    // ... "กลับไป" (Back) ให้ "หน้า Input" (AddTransactionFragment))
    public interface CategorySelectListener {
        void onCategorySelected(Category category);
    }

    private CategorySelectListener mListener;

    private RecyclerView recyclerView;
    private CategoryPickerAdapter adapter;
    private List<Category> categoryList;
    private DatabaseHelper dbHelper;

    // (เรา "บังคับ" (Force) ให้ "หน้า" (Fragment) ที่ "เรียก" (Call) Popup นี้...
    // ...ต้อง "คุม" (Implement) "Interface" (Interface) นี้... ไม่งั้น "พัง" (Crash))
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // (เราจะ "ตั้ง" (Set) 'AddTransactionFragment' ...
            // ...ให้เป็น "ผู้ฟัง" (Listener) ของเรา)
            // (ถ้า 'AddTransactionFragment' "เรียก" (Call) Fragment นี้... getParentFragment() จะ "ใช่" (Work))
            mListener = (CategorySelectListener) getParentFragment();
        } catch (ClassCastException e) {
            // (ถ้า "Activity" (Activity) (เช่น 'MainActivity') เป็นคน "เรียก" (Call)... ให้ใช้ 'context')
            try {
                mListener = (CategorySelectListener) context;
            } catch (ClassCastException e2) {
                throw new ClassCastException(context.toString()
                        + " หรือ ParentFragment ต้อง implement CategorySelectListener");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 1. "โหลด" (Inflate) "โครง XML" (XML Layout) 'fragment_manual_picker.xml' (v10.0)
        View view = inflater.inflate(R.layout.fragment_manual_picker, container, false);

        // 2. "เชื่อม" (Connect) "Database" (Database)
        dbHelper = new DatabaseHelper(getContext());

        // 3. "เชื่อม" (Connect) "RecyclerView" (RecyclerView) (ตาราง Grid)
        recyclerView = view.findViewById(R.id.recycler_category_picker);

        // 4. "ดึง" (Load) "ข้อมูล" (Data)
        loadCategoriesFromDatabase();

        // 5. "สร้าง" (Create) "ผู้ช่วย" (Adapter)
        // (เรา "ส่ง" (Pass) 'this' (Fragment นี้) ... เข้าไปเป็น "ผู้ฟัง" (Listener))
        adapter = new CategoryPickerAdapter(getContext(), categoryList, this);

        // 6. "ตั้งค่า" (Setup) "ตาราง" (Grid)
        // (นี่คือ "หัวใจ" (Core) ที่ "เปลี่ยน" (Change) "ลิสต์" (List) ... ให้เป็น "ตาราง 3 คอลัมน์" (3-Column Grid))
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        return view;
    }

    /**
     * "ดึง" (Fetch) "ลิสต์ 11 หมวดหมู่" (11 Categories List) จาก "SQLite" (SQLite)
     */
    private void loadCategoriesFromDatabase() {
        categoryList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // (เรา "ไม่" (Do NOT) เอา "รายรับ" (Income) ... มา "โชว์" (Show) ใน "Picker รายจ่าย" (Expense Picker))
        String selection = DatabaseHelper.COL_CAT_NAME + " != ?";
        String[] selectionArgs = {"รายรับ"}; // (หรือ "Income" ... ต้อง "ตรง" (Match) กับ 'seedCategories')

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CATEGORIES,
                null, // (เอา "ทุก" (All) คอลัมน์)
                selection, // (เอา "เฉพาะ" (Only) รายจ่าย (Expenses))
                selectionArgs,
                null, null, null
        );

        // "วนลูป" (Loop) "ข้อมูล" (Data)
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME));
            String iconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ICON));

            categoryList.add(new Category(id, name, iconName));
        }
        cursor.close();

        // (เรา "เพิ่ม" (Add) "ปุ่ม +" (Add Button) เข้าไป " manualmente" (Manually) ...
        // ... (ปุ่ม "Add" นี้ "ไม่" (Not) อยู่ใน "Database" (Database)))
        categoryList.add(new Category(-1, "+", "icon_add_plus")); // (ID -1 = "Add New")
    }

    /**
     * นี่คือ "Callback" (Callback) ... ที่ "ผู้ช่วย" (Adapter) ...
     * ...จะ "เรียก" (Call) ... เมื่อ User "คลิก" (Click) ไอคอน
     */
    @Override
    public void onCategoryClick(Category category) {
        if (category.getId() == -1) {
            // (ถ้า User "คลิก" (Click) "ปุ่ม +")
            // (อนาคต: "เปิด" (Open) "หน้าสร้าง Category ใหม่")
            // (MVP 6 วัน: "ยังไม่" (Not yet) ทำอะไร)
        } else {
            // (ถ้า User "คลิก" (Click) "Food" หรือ "Shopping"...)
            // 1. "ส่ง" (Send) "Category" (Category) (Food) ... "กลับไป" (Back) ให้ "หน้า Input" (AddTransactionFragment)
            mListener.onCategorySelected(category);

            // 2. "ปิด" (Close) "Popup" (Popup)
            dismiss();
        }
    }
}