package com.example.keeptang.logic;

import android.text.TextUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCategorizer {

    // --- รายจ่าย (Expense) ---
    private static final int ID_FOOD = 1;
    private static final int ID_TRAVEL = 2;
    private static final int ID_SHOPPING = 3;
    private static final int ID_BILLS = 4;
    private static final int ID_ENTERTAINMENT = 5;
    private static final int ID_HEALTH = 6;
    private static final int ID_HOME = 7;
    private static final int ID_EDUCATION = 8;
    private static final int ID_GIFTS = 9;

    // --- ✅ แก้ไข: เพิ่มรายรับให้ครบ และขยับเลขให้ตรง Database ---
    private static final int ID_SALARY = 10;
    private static final int ID_BONUS = 11;
    private static final int ID_INVESTMENT = 12;
    private static final int ID_OTHER_INCOME = 13;

    // 🚨 จุดสำคัญ: Others ต้องเป็น 14 (เพื่อให้ตรงกับ SmartCategorizer)
    private static final int ID_OTHERS = 14;

    private static final Map<Integer, List<String>> KEYWORD_MAP = new HashMap<>();

    static {
        // (Keywords เดิม...)
        KEYWORD_MAP.put(ID_FOOD, Arrays.asList("7-11", "seven", "เซเว่น", "food", "อาหาร", "ข้าว", "ก๋วยเตี๋ยว", "น้ำ", "กาแฟ", "starbucks", "amazon", "cafe", "บุฟเฟต์", "หมูกระทะ", "ชาบู", "kfc", "mk", "bonchon", "swensen", "dairy queen", "lineman", "grabfood", "foodpanda", "ขนม", "เบเกอรี่", "omakase", "sushi")); // แอบเติม Omakase ให้ด้วยเพื่อความชัวร์
        KEYWORD_MAP.put(ID_TRAVEL, Arrays.asList("bts", "mrt", "arl", "รถไฟฟ้า", "แท็กซี่", "taxi", "grab", "bolt", "muve", "วิน", "มอไซค์", "รถเมล์", "ค่ารถ", "น้ำมัน", "gas", "shell", "ptt", "ทางด่วน", "toll"));
        KEYWORD_MAP.put(ID_SHOPPING, Arrays.asList("shopee", "lazada", "tiktok", "shein", "zara", "uniqlo", "hm", "h&m", "pomelo", "เสื้อ", "กางเกง", "รองเท้า", "กระเป๋า", "เครื่องสำอาง", "eveandboy", "watsons", "sephora", "central", "paragon", "themall", "lotus", "bigc", "top", "gourmet"));
        KEYWORD_MAP.put(ID_BILLS, Arrays.asList("ค่าไฟ", "ค่าน้ำ", "ค่าเน็ต", "internet", "wifi", "ais", "true", "dtac", "ค่าโทรศัพท์", "บัตรเครดิต", "credit card", "ประกัน", "insurance"));
        KEYWORD_MAP.put(ID_ENTERTAINMENT, Arrays.asList("netflix", "spotify", "youtube", "disney", "prime", "hbo", "ดูหนัง", "major", "sf", "game", "steam", "playstation", "nintendo", "เติมเกม", "rov", "valorant", "concert", "บัตรคอน"));
        KEYWORD_MAP.put(ID_HEALTH, Arrays.asList("ยา", "pharmacy", "boots", "โรงพยาบาล", "hospital", "หมอ", "หมอฟัน", "ทำฟัน", "แว่น", "ตัดแว่น", "ออกกำลังกาย", "fitness", "gym"));
        KEYWORD_MAP.put(ID_HOME, Arrays.asList("ค่าเช่า", "rent", "ค่าส่วนกลาง", "condo", "ikea", "homepro", "index", "ของใช้", "ซ่อม"));
        KEYWORD_MAP.put(ID_EDUCATION, Arrays.asList("ค่าเทอม", "tuition", "หนังสือ", "book", "kinokuniya", "naiin", "b2s", "ชีท", "คอร์ส", "เรียน"));
        KEYWORD_MAP.put(ID_GIFTS, Arrays.asList("ของขวัญ", "gift", "ใส่ซอง", "งานแต่ง", "บริจาค", "donate", "ทำบุญ", "ให้แม่", "ให้พ่อ"));

        // ✅ Keywords รายรับ
        KEYWORD_MAP.put(ID_SALARY, Arrays.asList("เงินเดือน", "salary", "wage", "payroll", "เงินเข้า", "รายได้"));
        KEYWORD_MAP.put(ID_BONUS, Arrays.asList("โบนัส", "bonus", "อั่งเปา", "แต๊ะเอีย", "รางวัล", "ถูกหวย", "lotto"));
        KEYWORD_MAP.put(ID_INVESTMENT, Arrays.asList("หุ้น", "stock", "ปันผล", "dividend", "ดอกเบี้ย", "interest", "crypto", "bitcoin", "btc", "eth", "เทรด"));
    }

    public static int guessCategory(String itemName) {
        if (TextUtils.isEmpty(itemName)) {
            return ID_OTHERS;
        }
        String input = itemName.toLowerCase().trim();

        for (Map.Entry<Integer, List<String>> entry : KEYWORD_MAP.entrySet()) {
            int categoryId = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (input.contains(keyword)) {
                    return categoryId;
                }
            }
        }
        // ถ้าหาไม่เจอในกฎ -> คืนค่า 14 (Others)
        // (เมื่อ SmartCategorizer เห็นเลข 14 มันถึงจะยอมส่งต่อให้ Cloud AI)
        return ID_OTHERS;
    }
}