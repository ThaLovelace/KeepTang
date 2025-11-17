package com.example.keeptang.logic;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCategorizer {

    // ID ต้องตรงกับ DatabaseHelper
    private static final int ID_FOOD = 1;
    private static final int ID_TRAVEL = 2;
    private static final int ID_SHOPPING = 3;
    private static final int ID_BILLS = 4;
    private static final int ID_ENTERTAINMENT = 5;
    private static final int ID_HEALTH = 6;
    private static final int ID_HOME = 7;
    private static final int ID_EDUCATION = 8;
    private static final int ID_GIFTS = 9;
    private static final int ID_OTHERS = 11;

    // สร้าง "พจนานุกรม" (Dictionary) เก็บคำศัพท์
    private static final Map<Integer, List<String>> KEYWORD_MAP = new HashMap<>();

    static {
        // 1. อาหาร & เครื่องดื่ม (Food)
        KEYWORD_MAP.put(ID_FOOD, Arrays.asList(
                "7-11", "seven", "เซเว่น", "food", "อาหาร", "ข้าว", "ก๋วยเตี๋ยว", "น้ำ", "กาแฟ",
                "starbucks", "amazon", "cafe", "บุฟเฟต์", "หมูกระทะ", "ชาบู", "kfc", "mk", "bonchon",
                "swensen", "dairy queen", "lineman", "grabfood", "foodpanda", "ขนม", "เบเกอรี่"
        ));

        // 2. การเดินทาง (Travel)
        KEYWORD_MAP.put(ID_TRAVEL, Arrays.asList(
                "bts", "mrt", "arl", "รถไฟฟ้า", "แท็กซี่", "taxi", "grab", "bolt", "muve",
                "วิน", "มอไซค์", "รถเมล์", "ค่ารถ", "น้ำมัน", "gas", "shell", "ptt", "ทางด่วน", "toll"
        ));

        // 3. ช้อปปิ้ง (Shopping)
        KEYWORD_MAP.put(ID_SHOPPING, Arrays.asList(
                "shopee", "lazada", "tiktok", "shein", "zara", "uniqlo", "hm", "h&m", "pomelo",
                "เสื้อ", "กางเกง", "รองเท้า", "กระเป๋า", "เครื่องสำอาง", "eveandboy", "watsons", "sephora",
                "central", "paragon", "themall", "lotus", "bigc", "top", "gourmet"
        ));

        // 4. บิล & ค่าบริการ (Bills)
        KEYWORD_MAP.put(ID_BILLS, Arrays.asList(
                "ค่าไฟ", "ค่าน้ำ", "ค่าเน็ต", "internet", "wifi", "ais", "true", "dtac", "ค่าโทรศัพท์",
                "บัตรเครดิต", "credit card", "ประกัน", "insurance"
        ));

        // 5. บันเทิง (Entertainment)
        KEYWORD_MAP.put(ID_ENTERTAINMENT, Arrays.asList(
                "netflix", "spotify", "youtube", "disney", "prime", "hbo", "ดูหนัง", "major", "sf",
                "game", "steam", "playstation", "nintendo", "เติมเกม", "rov", "valorant", "concert", "บัตรคอน"
        ));

        // 6. สุขภาพ (Health)
        KEYWORD_MAP.put(ID_HEALTH, Arrays.asList(
                "ยา", "pharmacy", "boots", "โรงพยาบาล", "hospital", "หมอ", "หมอฟัน", "ทำฟัน",
                "แว่น", "ตัดแว่น", "ออกกำลังกาย", "fitness", "gym"
        ));

        // 7. ที่อยู่อาศัย (Home)
        KEYWORD_MAP.put(ID_HOME, Arrays.asList(
                "ค่าเช่า", "rent", "ค่าส่วนกลาง", "condo", "ikea", "homepro", "index", "ของใช้", "ซ่อม"
        ));

        // 8. การศึกษา (Education)
        KEYWORD_MAP.put(ID_EDUCATION, Arrays.asList(
                "ค่าเทอม", "tuition", "หนังสือ", "book", "kinokuniya", "naiin", "b2s", "ชีท", "คอร์ส", "เรียน"
        ));

        // 9. ของขวัญ (Gift)
        KEYWORD_MAP.put(ID_GIFTS, Arrays.asList(
                "ของขวัญ", "gift", "ใส่ซอง", "งานแต่ง", "บริจาค", "donate", "ทำบุญ", "ให้แม่", "ให้พ่อ"
        ));
    }

    /**
     * ฟังก์ชันหลักที่เรียกใช้จากภายนอก
     */
    public static int guessCategory(String itemName) {
        if (TextUtils.isEmpty(itemName)) {
            return ID_OTHERS;
        }

        String input = itemName.toLowerCase().trim();

        // วนลูปตรวจสอบทุกหมวดหมู่
        for (Map.Entry<Integer, List<String>> entry : KEYWORD_MAP.entrySet()) {
            int categoryId = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                // 1. เช็คแบบตรงตัว (Exact Match & Contains)
                if (input.contains(keyword)) {
                    return categoryId;
                }

                // 2. เช็คแบบ "ใกล้เคียง" (Fuzzy Match) สำหรับคำภาษาอังกฤษ
                // (เช่น user พิมพ์ "shoppee" แต่คีย์เวิร์ดคือ "shopee")
                if (isFuzzyMatch(input, keyword)) {
                    return categoryId;
                }
            }
        }

        return ID_OTHERS;
    }

    /**
     * ฟังก์ชันตรวจสอบคำใกล้เคียง (Levenshtein Distance แบบง่าย)
     * ยอมรับการพิมพ์ผิดได้ 1-2 ตัวอักษร สำหรับคำที่ยาวพอ
     */
    private static boolean isFuzzyMatch(String input, String keyword) {
        // ถ้าคำสั้นเกินไป ไม่ควรเดา (เดี๋ยวเพี้ยน)
        if (keyword.length() < 4) return false;

        // ถ้า input สั้นกว่า keyword มากๆ ก็ไม่ใช่แน่นอน
        if (Math.abs(input.length() - keyword.length()) > 2) return false;

        int distance = getLevenshteinDistance(input, keyword);

        // ยอมรับความผิดพลาดได้ 1 ตัวอักษร (เช่นพิมพ์ผิด 1 ตัว)
        return distance <= 1;
    }

    // อัลกอริทึมหาความต่างของคำ (Standard Algorithm)
    private static int getLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}