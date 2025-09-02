package com.ow0b.c7b9.app.old;

public class UserStats {
    private String userName;
    private int aiCallCount;

    public UserStats(String userName, int aiCallCount) {
        this.userName = userName;
        this.aiCallCount = aiCallCount;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getAiCallCount() {
        return aiCallCount;
    }

    public void setAiCallCount(int aiCallCount) {
        this.aiCallCount = aiCallCount;
    }
}