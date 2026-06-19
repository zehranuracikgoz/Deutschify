package com.zehranur.deutschifyapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule=
            new ActivityScenarioRule<>(LoginActivity.class);
    @Test
    public void test_loginScreen_displaysRequiredFields() {
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
    }
    @Test
    public void test_clickRegisterLink_navigatesToRegisterActivity() {
        onView(withId(R.id.tv_go_to_register)).perform(click());
        onView(withId(R.id.et_username)).check(matches(isDisplayed()));
    }
}