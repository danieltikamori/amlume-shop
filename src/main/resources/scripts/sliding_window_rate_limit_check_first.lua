-- Sliding Window Rate Limiter (Check Before Add)
-- Keys:
-- KEYS[1] = rate limit key (e.g., rate_limit:asn:1.2.3.4)
-- Args:
-- ARGV[1] = window size (in milliseconds)
-- ARGV[2] = max requests per window
-- ARGV[3] = current timestamp (in milliseconds)

local key = KEYS[1]
local windowMillis = tonumber(ARGV[1])
local maxRequests = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local windowStart = now - windowMillis

-- Clean up old entries (remove scores less than windowStart)
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Get current count BEFORE adding the new request
local currentCount = redis.call('ZCARD', key)

-- Check limit BEFORE adding
if currentCount >= maxRequests then
  return 0 -- Indicate limit exceeded
end

-- Add current request timestamp ONLY if limit is not exceeded
redis.call('ZADD', key, now, now)
-- Set expiry on the key to clean up if inactive (add buffer second)
redis.call('EXPIRE', key, math.floor(windowMillis / 1000) + 1)

return 1 -- Indicate success (request allowed and added)