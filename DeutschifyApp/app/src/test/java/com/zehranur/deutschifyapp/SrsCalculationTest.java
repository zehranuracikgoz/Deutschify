package com.zehranur.deutschifyapp;

import com.zehranur.deutschifyapp.util.SrsCalculator;
import org.junit.Test;
import static org.junit.Assert.*;

public class SrsCalculationTest {

    @Test
    public void test_quality5_increases_interval() {
        // rep=1 (2. tekrar) ile quality=5 verilince interval 6'ya çıkar (>1)
        SrsCalculator.Result result = SrsCalculator.calculateNextReview(2.5, 1, 1, 5);
        assertTrue("quality=5 should increase interval above 1", result.intervalDays > 1);
    }

    @Test
    public void test_quality1_resets_interval() {
        // quality < 3 → interval=1 ve rep=0'a sıfırlanmalı
        SrsCalculator.Result result = SrsCalculator.calculateNextReview(2.5, 6, 2, 1);
        assertEquals("interval should reset to 1", 1, result.intervalDays);
        assertEquals("repetition should reset to 0", 0, result.repetitionCount);
    }

    @Test
    public void test_ease_factor_minimum() {
        // Çok düşük ef + düşük quality -> ef asla 1.3'ün altına düşmemeli
        SrsCalculator.Result result = SrsCalculator.calculateNextReview(1.3, 1, 0, 1);
        assertTrue("ease_factor minimum is 1.3", result.easeFactor >= 1.3);
    }
}