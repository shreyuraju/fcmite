package com.mite.mitefc;

public class Trans {
    String USN, amount, date, mode, utr;

    public Trans(String USN, String amount, String date, String mode, String utr) {
        this.USN = USN;
        this.amount = amount;
        this.date = date;
        this.mode = mode;
        this.utr = utr;
    }

    public Trans() {
    }

    public String getUSN() {
        return USN;
    }

    public void setUSN(String USN) {
        this.USN = USN;
    }
    public String getAmount() {
        return amount;
    }
    public void setAmount(String amount) {
        this.amount = amount;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public String getUtr() {
        return utr;
    }
    public void setUtr(String utr) {
        this.utr = utr;
    }
}
