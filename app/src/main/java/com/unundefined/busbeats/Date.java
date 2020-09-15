package com.unundefined.busbeats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;

class Date {
    private LocalDate localDate;

    Date(int year, int month, int day) {
        this.localDate = LocalDate.of(year, month, day);
    }


    @Nullable
    static Date toDate(String dateString) {
        String[] numbers = dateString.split("-");
        int day = Integer.parseInt(numbers[0]);
        int month = Integer.parseInt(numbers[1]);
        int year = Integer.parseInt(numbers[2]);

        if (month > 12 && day <= 12) {
            int temp = day;
            day = month;
            month = temp;
        } else if (month > 12) {
            return null;
        }
        return new Date(year, month, day);
    }

    @NonNull
    @Override
    public String toString() {
        return localDate.toString();
    }
}
