package com.sait.peelin.service;

public interface TokenDenylistService {
    void deny(String token);
    boolean isDenied(String token);
}
