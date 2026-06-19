package com.zehranur.deutschifyapp;

import com.zehranur.deutschifyapp.util.InputValidator;
import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationTest {

    @Test
    public void test_empty_email_invalid() {
        assertFalse("boş string geçersiz email", InputValidator.isValidEmail(""));
        assertFalse("null geçersiz email", InputValidator.isValidEmail(null));
        assertFalse("sadece boşluk geçersiz email", InputValidator.isValidEmail("   "));
    }

    @Test
    public void test_valid_email_format() {
        assertTrue("test@test.com geçerli", InputValidator.isValidEmail("test@test.com"));
        assertTrue("user@domain.org geçerli", InputValidator.isValidEmail("user@domain.org"));
        assertFalse("@eksik local part geçersiz", InputValidator.isValidEmail("@test.com"));
        assertFalse("domain yok geçersiz", InputValidator.isValidEmail("test@"));
    }

    @Test
    public void test_quality_range() {
        assertTrue("1 geçerli", InputValidator.isValidQuality(1)) ;
        assertTrue("3 geçerli", InputValidator.isValidQuality(3));
        assertTrue("5 geçerli", InputValidator.isValidQuality(5));
        assertFalse("0 geçersiz", InputValidator.isValidQuality(0));
        assertFalse("6 geçersiz", InputValidator.isValidQuality(6));
    }
}