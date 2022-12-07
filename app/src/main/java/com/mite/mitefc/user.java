package com.mite.mitefc;

public class user {
    public String USN;
    public String balance;

    public user(String USN, String balance) {
        this.USN = USN;
        this.balance = balance;
    }

    public user() {
    }

    public String getUSN() {
        return USN;
    }

    public void setUSN(String USN) {
        this.USN = USN;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}
