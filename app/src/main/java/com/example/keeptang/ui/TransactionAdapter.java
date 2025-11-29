package com.example.keeptang.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.keeptang.R;
import com.example.keeptang.data.Transaction;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList;

    // ✅ 1. เพิ่ม Interface สำหรับการคลิก
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }
    private OnItemClickListener listener;

    // ✅ 2. อัปเดต Constructor ให้รับ Listener
    public TransactionAdapter(Context context, List<Transaction> transactionList, OnItemClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
    }

    // (Constructor เก่า - เผื่อโค้ดอื่นเรียกใช้แบบไม่ส่ง listener)
    public TransactionAdapter(Context context, List<Transaction> transactionList) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = null;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.tvItemName.setText(transaction.getName());
        holder.tvItemCategory.setText(transaction.getCategoryName());

        int iconResId = context.getResources().getIdentifier(
                transaction.getCategoryIconName(), "drawable", context.getPackageName());
        holder.ivItemIcon.setImageResource(iconResId != 0 ? iconResId : R.drawable.icon_edit);

        double price = transaction.getPrice();
        holder.tvItemAmount.setText(String.format(Locale.getDefault(), "฿ %,.2f", price));

        // ✅✅✅ แก้ไข: ใช้สีดำตลอด (ไม่ว่าบวกหรือลบ) ✅✅✅
        holder.tvItemAmount.setTextColor(ContextCompat.getColor(context, R.color.black));

        // ✅ 3. ดักฟังการคลิกที่ทั้งแถว
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemIcon;
        TextView tvItemName;
        TextView tvItemCategory;
        TextView tvItemAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemIcon = itemView.findViewById(R.id.item_icon);
            tvItemName = itemView.findViewById(R.id.item_name);
            tvItemCategory = itemView.findViewById(R.id.item_category);
            tvItemAmount = itemView.findViewById(R.id.item_amount);
        }
    }
}