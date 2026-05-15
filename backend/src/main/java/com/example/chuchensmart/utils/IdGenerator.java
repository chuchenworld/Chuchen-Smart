package com.example.chuchensmart.utils;

import com.example.chuchensmart.common.SnowflakeIdGenerator;

public class IdGenerator {
    
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator(1, 1);
    
    public static long generateId() {
        return ID_GENERATOR.nextId();
    }
}