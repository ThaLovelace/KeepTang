package com.example.keeptang.logic;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Gamification {

    private static final String PREFS_NAME = "KeepTangPrefs";
    private static final String KEY_TOTAL_POINTS = "total_points";
    private static final String KEY_LAST_CHECKIN_DATE = "last_checkin_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";

    // กติกาการแจกแต้ม
    private static final int POINTS_DAILY = 10;       // เข้าปกติได้ 10
    private static final int POINTS_WEEKLY_BONUS = 50; // ครบ 7 วัน ได้โบนัส 50
    private static final int DEFAULT_POINTS = 100;    // แต้มเริ่มต้นสำหรับผู้ใช้ใหม่

    private SharedPreferences sharedPrefs;

    public Gamification(Context context) {
        this.sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTotalPoints() {
        return sharedPrefs.getInt(KEY_TOTAL_POINTS, DEFAULT_POINTS);
    }

    // คลาสสำหรับส่งผลลัพธ์กลับไปบอกหน้าจอ
    public static class CheckInResult {
        public final boolean isNewCheckIn; // วันนี้เพิ่งเข้าครั้งแรกใช่ไหม?
        public final int pointsEarned;     // ได้กี่แต้มรอบนี้
        public final int totalPoints;      // แต้มรวมตอนนี้
        public final int streak;           // ต่อเนื่องมากี่วันแล้ว

        public CheckInResult(boolean isNewCheckIn, int pointsEarned, int totalPoints, int streak) {
            this.isNewCheckIn = isNewCheckIn;
            this.pointsEarned = pointsEarned;
            this.totalPoints = totalPoints;
            this.streak = streak;
        }
    }

    // ✅ ฟังก์ชันช่วยคำนวณแต้มตามวัน (กฎใหม่)
    public int getPointsForDay(int day) {
        if (day == 5) return 10;      // วันที่ 5 ได้ 10
        if (day == 7) return 15;      // วันที่ 7 ได้ 15
        return 5;                     // วันอื่นๆ (1,2,3,4,6) ได้ 5
    }

    public CheckInResult performDailyCheckIn() {
        String lastDate = sharedPrefs.getString(KEY_LAST_CHECKIN_DATE, "");
        String todayDate = getTodayDate();
        int currentStreak = sharedPrefs.getInt(KEY_CURRENT_STREAK, 0);
        int currentPoints = getTotalPoints();

        // 1. ถ้าวันนี้เช็คอินไปแล้ว
        if (lastDate.equals(todayDate)) {
            return new CheckInResult(false, 0, currentPoints, currentStreak);
        }

        // 2. คำนวณ Streak
        String yesterdayDate = getYesterdayDate();
        if (lastDate.equals(yesterdayDate)) {
            currentStreak++;
        } else {
            currentStreak = 1; // เริ่มนับ 1 ใหม่
        }

        // ถ้าเกิน 7 วัน ให้วนกลับมาวันที่ 1
        if (currentStreak > 7) currentStreak = 1;

        // 3. คำนวณแต้ม (ใช้ฟังก์ชันใหม่)
        int pointsToAdd = getPointsForDay(currentStreak);

        int newTotalPoints = currentPoints + pointsToAdd;

        // 4. บันทึก
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(KEY_TOTAL_POINTS, newTotalPoints);
        editor.putString(KEY_LAST_CHECKIN_DATE, todayDate);
        editor.putInt(KEY_CURRENT_STREAK, currentStreak);
        editor.apply();

        return new CheckInResult(true, pointsToAdd, newTotalPoints, currentStreak);
    }

    // ฟังก์ชันช่วยหาวันที่
    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }
}
