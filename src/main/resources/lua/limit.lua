-- KEYS[1]: 限流Key (如 dedup:marketing:10086)
-- ARGV[1]: 当前时间戳 (毫秒)
-- ARGV[2]: 窗口时间 (毫秒)
-- ARGV[3]: 阈值 (次数)
-- ARGV[4]: 唯一标识 (如 UUID，防止ZSET元素覆盖)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]

-- 1. 移除窗口之外的旧数据
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 2. 统计当前窗口内的数量
local count = redis.call('ZCARD', key)

-- 3. 判断是否超限
if count < limit then
    -- 未超限：写入当前请求
    redis.call('ZADD', key, now, member)
    redis.call('PEXPIRE', key, window) -- 续期
    return 1 -- 允许
else
    return 0 -- 拒绝
end
