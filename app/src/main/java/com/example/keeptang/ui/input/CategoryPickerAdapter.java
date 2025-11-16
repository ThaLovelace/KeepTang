package com.example.keeptang.ui.input; // (หรือ Package name ของคุณ)

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keeptang.R;
import com.example.keeptang.data.Category;

import java.util.List;

// (นี่คือ "Adapter" ที่จะ "คุม" (Control) 'fragment_manual_picker.xml'
// ... และ "ไส้ใน" (Item) 'list_item_category_picker.xml')
public class CategoryPickerAdapter extends RecyclerView.Adapter<CategoryPickerAdapter.CategoryViewHolder> {

    private Context context;
    private List<Category> categoryList; // "ข้อมูล" (Data) ทั้งหมด (Food, Travel, etc.)
    private OnCategoryClickListener listener; // "ตัวสื่อสาร" (Communicator) (เดี๋ยวเรา "สร้าง" (Create) Interface นี้)

    // 1. "Interface" (ตัวสื่อสาร)
    // (เพื่อให้ "Popup" (Popup) "บอก" (Tell) "หน้า Input" (Input) ได้ว่า "User เลือกอะไร")
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    // 2. Constructor (ตัวสร้าง)
    public CategoryPickerAdapter(Context context, List<Category> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    // 3. "สร้าง" (Create) "ช่อง" (View Holder)
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "โหลด" (Inflate) "ไส้ใน" (Item Layout) 'list_item_category_picker.xml' (v11.0)
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_category_picker, parent, false);
        return new CategoryViewHolder(view);
    }

    // 4. "ยัด" (Bind) "ข้อมูล" (Data)
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        // "ดึง" (Get) "Category" (Category) ทีละอัน...
        Category category = categoryList.get(position);

        // "ยัด" (Set) "ชื่อ" (Name)
        holder.tvPickerName.setText(category.getName());

        // "ยัด" (Set) "ไอคอนน่ารัก" (Cute Icon)
        // (นี่คือ "โค้ด" (Code) ที่ "แปลง" (Convert) "ชื่อ" (String) (เช่น 'icon_food')...
        // ...ไปเป็น "รูป" (Drawable) จริง)
        int iconResId = context.getResources().getIdentifier(
                category.getIconName(),
                "drawable",
                context.getPackageName()
        );
        holder.ivPickerIcon.setImageResource(iconResId);

        // 5. "ดักฟัง" (Listen) "การคลิก" (Click)
        holder.itemView.setOnClickListener(v -> {
            // (ถ้า User "คลิก" (Click) ที่ "Food"...)
            // (... "เรียก" (Call) "Interface" (Interface) ... "ส่ง" (Send) "Food" (Food) กลับไป)
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }


    // "ตัวจับ" (View Holder) (ที่ "เชื่อม" (Connect) XML กับ Java)
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPickerIcon;
        TextView tvPickerName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // "เชื่อม" (Connect) "ไส้ใน" (Item) ('list_item_category_picker.xml')
            ivPickerIcon = itemView.findViewById(R.id.picker_icon);
            tvPickerName = itemView.findViewById(R.id.picker_name);
        }
    }
}