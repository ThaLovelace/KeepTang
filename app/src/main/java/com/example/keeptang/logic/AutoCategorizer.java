package com.example.keeptang.logic; // (หรือ Package name ของคุณ)

import android.text.TextUtils;

public class AutoCategorizer {

    // --- นี่คือ "พิมพ์เขียว" (Schema) ID จาก Database ---
    // (สำคัญมาก! ID นี้ต้อง "ตรง" (Match) กับ "ลำดับ" (Order)
    // ที่เรา "INSERT" (Seeded) ไว้ใน 'DatabaseHelper.java')

    private static final int ID_FOOD = 1;
    private static final int ID_TRAVEL = 2;
    private static final int ID_SHOPPING = 3;
    private static final int ID_BILLS = 4;
    private static final int ID_ENTERTAINMENT = 5;
    private static final int ID_HEALTH = 6;
    private static final int ID_HOME = 7;
    private static final int ID_EDUCATION = 8;
    private static final int ID_GIFTS = 9;
    private static final int ID_INCOME = 10; // (เรา "ไม่" (Won't) เดาอันนี้... User ต้อง "เลือก" (Select) แท็บ)
    private static final int ID_OTHERS = 11; // (หมวดหมู่ Default)


    /**
     * นี่คือ "สมอง AI" (AI Brain)
     * มัน "รับ" (Takes) 'itemName' (เช่น "7-Eleven")
     * และ "คืน" (Returns) 'category_id' (เช่น 1)
     * (อ้างอิง Flow "Smart Input")
     */
    public static int guessCategory(String itemName) {

        // 1. "ป้องกัน" (Prevent) Bug... ถ้า "ว่าง" (Empty) หรือ "Null"
        if (TextUtils.isEmpty(itemName)) {
            return ID_OTHERS; // (คืนค่า "Others" ทันที)
        }

        // 2. "แปลง" (Convert) เป็น "พิมพ์เล็ก" (Lowercase) ...
        // (เพื่อ "จับ" (Match) "7-Eleven" และ "7-eleven" ได้)
        String input = itemName.toLowerCase();


        // 3. "กฎ" (The Rules) ... (อ้างอิง "ลิสต์ 11 หมวดหมู่")
        // (เราจะ "เรียง" (Order) จาก "เฉพาะเจาะจง" (Specific) ไป "ทั่วไป" (General))

        // --- (หมวด 2: การเดินทาง) ---
        if (input.contains("bts") || input.contains("mrt") ||
                input.contains("grab") || input.contains("bolt") ||
                input.contains("แท็กซี่") || input.contains("ค่าน้ำมัน")) {
            return ID_TRAVEL;
        }

        // --- (หมวด 1: อาหาร) ---
        // (เราเช็ก "Grab" (Travel) "ก่อน" (Before) "GrabFood" (Food) ...
        // ...แต่ถ้าชื่อ "GrabFood"... มันจะ "ไม่" (Not) โดนจับที่ "Grab" (Travel) ...
        // ...และ "หล่น" (Fall through) มา "โดนจับ" (Be caught) ที่ "food" (Food) แทน)
        if (input.contains("food") || input.contains("7-eleven") ||
                input.contains("กาแฟ") || input.contains("ข้าว") ||
                input.contains("starbucks") || input.contains("amazon") ||
                input.contains("lineman")) {
            return ID_FOOD;
        }

        // --- (หมวด 3: ช้อปปิ้ง) ---
        if (input.contains("shopee") || input.contains("lazada") ||
                input.contains("uniqlo") || input.contains("central") ||
                input.contains("เสื้อผ้า") || input.contains("eveandboy")) {
            return ID_SHOPPING;
        }

        // --- (หมวด 4: บิล) ---
        if (input.contains("ค่าไฟ") || input.contains("ค่าน้ำ") ||
                input.contains("ค่าเน็ต") || input.contains("ais") ||
                input.contains("true") || input.contains("dtac") ||
                input.contains("ค่าโทรศัพท์")) {
            return ID_BILLS;
        }

        // --- (หมวด 5: บันเทิง) ---
        if (input.contains("netflix") || input.contains("spotify") ||
                input.contains("major") || input.contains("sf") ||
                input.contains("ดูหนัง") || input.contains("คอนเสิร์ต") ||
                input.contains("เกม")) {
            return ID_ENTERTAINMENT;
        }

        // --- (หมวด 6: สุขภาพ) ---
        if (input.contains("ค่ายา") || input.contains("โรงพยาบาล") ||
                input.contains("หมอ") || input.contains("boots") ||
                input.contains("watsons")) {
            return ID_HEALTH;
        }

        // --- (หมวด 7: ที่อยู่อาศัย) ---
        if (input.contains("ค่าเช่า") || input.contains("คอนโด") ||
                input.contains("ikea") || input.contains("big c") ||
                input.contains("lotus")) {
            return ID_HOME;
        }

        // --- (หมวด 8: การศึกษา) ---
        if (input.contains("ค่าเทอม") || input.contains("หนังสือ") ||
                input.contains("b2s") || input.contains("เครื่องเขียน")) {
            return ID_EDUCATION;
        }

        // --- (หมวด 9: ของขวัญ) ---
        if (input.contains("ของขวัญ") || input.contains("ทำบุญ") ||
                input.contains("บริจาค")) {
            return ID_GIFTS;
        }

        // --- (Default) ---
        // (ถ้า "ไม่" (Not) ตรงกับ "กฎ" (Rule) ไหนเลย...)
        return ID_OTHERS; // (คืนค่า "อื่น ๆ")
    }
}