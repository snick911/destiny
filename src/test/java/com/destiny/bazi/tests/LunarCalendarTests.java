package com.destiny.bazi.tests;

import com.destiny.bazi.core.LunarCalendar;

import java.time.LocalDate;

/**
 * 農曆換算測試：與查表法逐日比對 1900~2100 年，並檢查春節、閏月、往返換算與中文寫法。
 *
 * <p>對照用的曆表資料取自 lifeup 專案的 {@code cn.z.LunarSolarConverter}（依香港天文台
 * 公布的農曆編碼），解碼程式為本測試另行撰寫，與 {@link LunarCalendar} 的天文推算互相獨立。</p>
 */
final class LunarCalendarTests {

    /** 各農曆年（自 1887 年起）的月大小與閏月：低 13 位元由高到低為各月（1＝大月 30 天），再上 4 位元為閏月月序。 */
    private static final int[] MONTH_DAYS = {
            1887, 0x1694, 0x16aa, 0x4ad5, 0xab6, 0xc4b7, 0x4ae, 0xa56, 0xb52a, 0x1d2a, 0xd54, 0x75aa, 0x156a,
            0x1096d, 0x95c, 0x14ae, 0xaa4d, 0x1a4c, 0x1b2a, 0x8d55, 0xad4, 0x135a, 0x495d, 0x95c, 0xd49b, 0x149a,
            0x1a4a, 0xbaa5, 0x16a8, 0x1ad4, 0x52da, 0x12b6, 0xe937, 0x92e, 0x1496, 0xb64b, 0xd4a, 0xda8, 0x95b5,
            0x56c, 0x12ae, 0x492f, 0x92e, 0xcc96, 0x1a94, 0x1d4a, 0xada9, 0xb5a, 0x56c, 0x726e, 0x125c, 0xf92d,
            0x192a, 0x1a94, 0xdb4a, 0x16aa, 0xad4, 0x955b, 0x4ba, 0x125a, 0x592b, 0x152a, 0xf695, 0xd94, 0x16aa,
            0xaab5, 0x9b4, 0x14b6, 0x6a57, 0xa56, 0x1152a, 0x1d2a, 0xd54, 0xd5aa, 0x156a, 0x96c, 0x94ae, 0x14ae,
            0xa4c, 0x7d26, 0x1b2a, 0xeb55, 0xad4, 0x12da, 0xa95d, 0x95a, 0x149a, 0x9a4d, 0x1a4a, 0x11aa5, 0x16a8,
            0x16d4, 0xd2da, 0x12b6, 0x936, 0x9497, 0x1496, 0x1564b, 0xd4a, 0xda8, 0xd5b4, 0x156c, 0x12ae, 0xa92f,
            0x92e, 0xc96, 0x6d4a, 0x1d4a, 0x10d65, 0xb58, 0x156c, 0xb26d, 0x125c, 0x192c, 0x9a95, 0x1a94, 0x1b4a,
            0x4b55, 0xad4, 0xf55b, 0x4ba, 0x125a, 0xb92b, 0x152a, 0x1694, 0x96aa, 0x15aa, 0x12ab5, 0x974, 0x14b6,
            0xca57, 0xa56, 0x1526, 0x8e95, 0xd54, 0x15aa, 0x49b5, 0x96c, 0xd4ae, 0x149c, 0x1a4c, 0xbd26, 0x1aa6,
            0xb54, 0x6d6a, 0x12da, 0x1695d, 0x95a, 0x149a, 0xda4b, 0x1a4a, 0x1aa4, 0xbb54, 0x16b4, 0xada, 0x495b,
            0x936, 0xf497, 0x1496, 0x154a, 0xb6a5, 0xda4, 0x15b4, 0x6ab6, 0x126e, 0x1092f, 0x92e, 0xc96, 0xcd4a,
            0x1d4a, 0xd64, 0x956c, 0x155c, 0x125c, 0x792e, 0x192c, 0xfa95, 0x1a94, 0x1b4a, 0xab55, 0xad4, 0x14da,
            0x8a5d, 0xa5a, 0x1152b, 0x152a, 0x1694, 0xd6aa, 0x15aa, 0xab4, 0x94ba, 0x14b6, 0xa56, 0x7527, 0xd26,
            0xee53, 0xd54, 0x15aa, 0xa9b5, 0x96c, 0x14ae, 0x8a4e, 0x1a4c, 0x11d26, 0x1aa4, 0x1b54, 0xcd6a, 0xada,
            0x95c, 0x949d, 0x149a, 0x1a2a, 0x5b25, 0x1aa4, 0xfb52, 0x16b4, 0xaba, 0xa95b, 0x936, 0x1496, 0x9a4b,
            0x154a, 0x136a5, 0xda4, 0x15ac
    };

    /** 各農曆年（自 1887 年起）正月初一的西曆日期：(年 << 9) | (月 << 5) | 日。 */
    private static final int[] NEW_YEAR = {
            1887, 0xec04c, 0xec23f, 0xec435, 0xec649, 0xec83e, 0xeca51, 0xecc46, 0xece3a, 0xed04d, 0xed242, 0xed436,
            0xed64a, 0xed83f, 0xeda53, 0xedc48, 0xede3d, 0xee050, 0xee244, 0xee439, 0xee64d, 0xee842, 0xeea36,
            0xeec4a, 0xeee3e, 0xef052, 0xef246, 0xef43a, 0xef64e, 0xef843, 0xefa37, 0xefc4b, 0xefe41, 0xf0054,
            0xf0248, 0xf043c, 0xf0650, 0xf0845, 0xf0a38, 0xf0c4d, 0xf0e42, 0xf1037, 0xf124a, 0xf143e, 0xf1651,
            0xf1846, 0xf1a3a, 0xf1c4e, 0xf1e44, 0xf2038, 0xf224b, 0xf243f, 0xf2653, 0xf2848, 0xf2a3b, 0xf2c4f,
            0xf2e45, 0xf3039, 0xf324d, 0xf3442, 0xf3636, 0xf384a, 0xf3a3d, 0xf3c51, 0xf3e46, 0xf403b, 0xf424e,
            0xf4443, 0xf4638, 0xf484c, 0xf4a3f, 0xf4c52, 0xf4e48, 0xf503c, 0xf524f, 0xf5445, 0xf5639, 0xf584d,
            0xf5a42, 0xf5c35, 0xf5e49, 0xf603e, 0xf6251, 0xf6446, 0xf663b, 0xf684f, 0xf6a43, 0xf6c37, 0xf6e4b,
            0xf703f, 0xf7252, 0xf7447, 0xf763c, 0xf7850, 0xf7a45, 0xf7c39, 0xf7e4d, 0xf8042, 0xf8254, 0xf8449,
            0xf863d, 0xf8851, 0xf8a46, 0xf8c3b, 0xf8e4f, 0xf9044, 0xf9237, 0xf944a, 0xf963f, 0xf9853, 0xf9a47,
            0xf9c3c, 0xf9e50, 0xfa045, 0xfa238, 0xfa44c, 0xfa641, 0xfa836, 0xfaa49, 0xfac3d, 0xfae52, 0xfb047,
            0xfb23a, 0xfb44e, 0xfb643, 0xfb837, 0xfba4a, 0xfbc3f, 0xfbe53, 0xfc048, 0xfc23c, 0xfc450, 0xfc645,
            0xfc839, 0xfca4c, 0xfcc41, 0xfce36, 0xfd04a, 0xfd23d, 0xfd451, 0xfd646, 0xfd83a, 0xfda4d, 0xfdc43,
            0xfde37, 0xfe04b, 0xfe23f, 0xfe453, 0xfe648, 0xfe83c, 0xfea4f, 0xfec44, 0xfee38, 0xff04c, 0xff241,
            0xff436, 0xff64a, 0xff83e, 0xffa51, 0xffc46, 0xffe3a, 0x10004e, 0x100242, 0x100437, 0x10064b, 0x100841,
            0x100a53, 0x100c48, 0x100e3c, 0x10104f, 0x101244, 0x101438, 0x10164c, 0x101842, 0x101a35, 0x101c49,
            0x101e3d, 0x102051, 0x102245, 0x10243a, 0x10264e, 0x102843, 0x102a37, 0x102c4b, 0x102e3f, 0x103053,
            0x103247, 0x10343b, 0x10364f, 0x103845, 0x103a38, 0x103c4c, 0x103e42, 0x104036, 0x104249, 0x10443d,
            0x104651, 0x104846, 0x104a3a, 0x104c4e, 0x104e43, 0x105038, 0x10524a, 0x10543e, 0x105652, 0x105847,
            0x105a3b, 0x105c4f, 0x105e45, 0x106039, 0x10624c, 0x106441, 0x106635, 0x106849, 0x106a3d, 0x106c51,
            0x106e47, 0x10703c, 0x10724f, 0x107444, 0x107638, 0x10784c, 0x107a3f, 0x107c53, 0x107e48
    };

    private LunarCalendarTests() {
    }

    static void run() {
        compareWithTable();
        knownDates();
        roundTrip();
        dayNames();
    }

    /** 1900-01-31（庚子年正月初一）至 2100-12-31 逐日與曆表比對。 */
    private static void compareWithTable() {
        int mismatches = 0;
        for (int year = 1900; year <= 2100; year++) {
            int index = year - MONTH_DAYS[0];
            int data = MONTH_DAYS[index];
            int leap = (data >> 13) & 0xF;
            int count = leap == 0 ? 12 : 13;
            LocalDate date = decodeNewYear(NEW_YEAR[index]);
            for (int i = 0; i < count; i++) {
                int length = ((data >> (12 - i)) & 1) == 1 ? 30 : 29;
                boolean isLeap = leap != 0 && i == leap;
                int month = leap != 0 && i >= leap ? i : i + 1;
                for (int day = 1; day <= length; day++) {
                    LunarCalendar.LunarDate actual = LunarCalendar.fromSolar(date);
                    LunarCalendar.LunarDate expected = new LunarCalendar.LunarDate(year, month, isLeap, day);
                    if (!expected.equals(actual)) {
                        mismatches++;
                        Check.equal("農曆 " + date, expected, actual);
                    }
                    date = date.plusDays(1);
                }
            }
            if (year < 2100) {
                Check.equal(year + "年曆表的年長與次年春節一致", decodeNewYear(NEW_YEAR[index + 1]), date);
            }
            Check.equal(year + "年閏月", leap, LunarCalendar.leapMonth(year));
        }
        Check.equal("1900~2100 年逐日與曆表不一致的天數", 0, mismatches);
    }

    private static LocalDate decodeNewYear(int packed) {
        return LocalDate.of(packed >> 9, (packed >> 5) & 0xF, packed & 0x1F);
    }

    private static void knownDates() {
        // 春節（正月初一）
        Object[][] newYears = {
                {1900, 1, 31}, {1950, 2, 17}, {1984, 2, 2}, {2000, 2, 5}, {2020, 1, 25},
                {2023, 1, 22}, {2024, 2, 10}, {2025, 1, 29}, {2026, 2, 17}, {2100, 2, 9}
        };
        for (Object[] ny : newYears) {
            LocalDate date = LocalDate.of((int) ny[0], (int) ny[1], (int) ny[2]);
            Check.equal(date + " 為正月初一", new LunarCalendar.LunarDate((int) ny[0], 1, false, 1),
                    LunarCalendar.fromSolar(date));
            Check.equal(date + " 前一天為前一年臘月", 12, LunarCalendar.fromSolar(date.minusDays(1)).month());
        }
        // 閏月
        int[][] leaps = {{1984, 10}, {2001, 4}, {2017, 6}, {2020, 4}, {2023, 2}, {2025, 6}, {2033, 11}, {2024, 0}};
        for (int[] l : leaps) {
            Check.equal(l[0] + "年閏月", l[1], LunarCalendar.leapMonth(l[0]));
        }
        Check.equal("2023-03-22 閏二月初一", new LunarCalendar.LunarDate(2023, 2, true, 1),
                LunarCalendar.fromSolar(LocalDate.of(2023, 3, 22)));
        Check.equal("2033-12-22 閏十一月初一", new LunarCalendar.LunarDate(2033, 11, true, 1),
                LunarCalendar.fromSolar(LocalDate.of(2033, 12, 22)));
        // 1912~1928 年依北京地方平時：合朔在 UTC+8 午夜後數分鐘，曆書歸入前一天
        Check.equal("1916-02-03 丙辰年正月初一", new LunarCalendar.LunarDate(1916, 1, false, 1),
                LunarCalendar.fromSolar(LocalDate.of(1916, 2, 3)));
        // 合朔緊貼午夜、依公布曆表歸入次日
        Check.equal("2057-09-28 為八月三十", new LunarCalendar.LunarDate(2057, 8, false, 30),
                LunarCalendar.fromSolar(LocalDate.of(2057, 9, 28)));
        Check.equal("2026-11-17 丙午年十月初九", "丙午年十月初九",
                LunarCalendar.fromSolar(LocalDate.of(2026, 11, 17)).toChineseString());
    }

    private static void roundTrip() {
        for (LocalDate d = LocalDate.of(1900, 2, 1); d.getYear() <= 2100; d = d.plusDays(7)) {
            LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(d);
            Check.equal("往返換算 " + d, d, LunarCalendar.toSolar(lunar.year(), lunar.month(), lunar.leap(), lunar.day()));
        }
        Check.isTrue("不存在的閏月", throwsIllegalArgument(() -> LunarCalendar.toSolar(2024, 5, true, 1)), "2024 年沒有閏五月");
        Check.isTrue("小月沒有三十", throwsIllegalArgument(() -> LunarCalendar.toSolar(2023, 1, false, 30)),
                "2023 年正月只有 29 天");
    }

    private static boolean throwsIllegalArgument(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void dayNames() {
        String[] expected = {"初一", "初十", "十一", "十九", "二十", "廿一", "廿九", "三十"};
        int[] days = {1, 10, 11, 19, 20, 21, 29, 30};
        for (int i = 0; i < days.length; i++) {
            Check.equal("農曆日 " + days[i], expected[i], LunarCalendar.dayName(days[i]));
        }
        Check.equal("閏月名稱", "閏四月", new LunarCalendar.LunarDate(2020, 4, true, 1).monthName());
    }
}
