-- Keys:
-- KEYS[1] = rate-limit:{key} (e.g., rate-limit:token-validation)

-- Args:
-- ARGV[1] = current timestamp (in milliseconds)
-- ARGV[2] = window size (in milliseconds)
-- ARGV[3] = max requests per window

local currentTime = tonumber(ARGV[1])
local windowSize = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])

local key = KEYS[1]
local windowStart = currentTime - windowSize

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Add current timestamp
redis.call('ZADD', key, currentTime, currentTime)

-- Count requests in current window
local requestCount = redis.call('ZCARD', key)

-- Set expiry to clean up old windows
redis.call('EXPIRE', key, windowSize / 1000)

-- Check if rate limit is exceeded
if requestCount <= maxRequests then
    return 1
else
    return 0
end