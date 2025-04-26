/*
 * Copyright (c) 2025 Daniel Itiro Tikamori. All rights reserved.
 *
 * This software is proprietary, not intended for public distribution, open source, or commercial use. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means, electronic or mechanical, including photocopying, recording, or by any information storage or retrieval system, without the prior written permission of the copyright holder.
 *
 * Permission to use, copy, modify, and distribute this software is strictly prohibited without prior written authorization from the copyright holder.
 *
 * Please contact the copyright holder at echo ZnVpd3pjaHBzQG1vem1haWwuY29t | base64 -d && echo for any inquiries or requests for authorization to use the software.
 */

package me.amlu.shop.amlume_shop.resilience.service;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class LeakyBucket {
    private final Queue<Request> bucket;
    private final int capacity;
    private final int leakRate;
    private long lastLeakTime;
    
    public LeakyBucket(int capacity, int leakRate) {
        this.bucket = new LinkedBlockingQueue<>(capacity);
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.lastLeakTime = System.currentTimeMillis();
    }
    
    public synchronized boolean tryConsume() {
        leak();
        
        if (bucket.size() >= capacity) {
            return false;
        }
        
        return bucket.offer(new Request(System.currentTimeMillis()));
    }
    
    private void leak() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - lastLeakTime;
        int itemsToLeak = (int) (timeElapsed * leakRate / 1000);
        
        for (int i = 0; i < itemsToLeak && !bucket.isEmpty(); i++) {
            bucket.poll();
        }
        
        lastLeakTime = now;
    }
    
    private static class Request {
        private final long timestamp;
        
        Request(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
