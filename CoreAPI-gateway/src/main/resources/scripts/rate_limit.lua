-- KEYS[1]：限流 key
-- ARGV[1]：当前时间戳（毫秒）
-- ARGV[2]：滑动窗口大小（毫秒）
-- ARGV[3]：最大请求数

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- 删除过期请求记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 统计当前窗口内的请求数
local count = redis.call('ZCARD', key)

if count >= limit then
    -- 限流
    return 0
else
    -- 计数并刷新 key 的 TTL
    redis.call('ZADD', key, now, now .. math.random(0, 999))
    redis.call('PEXPIRE', key, window)
    return 1
end
