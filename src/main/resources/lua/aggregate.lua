-- KEYS[1]: 数据List Key (如 agg:data:userId:type)
-- KEYS[2]: 标记Flag Key (如 agg:flag:userId:type)
-- KEYS[3]: 计数Counter Key (如 agg:count:userId:type)
-- ARGV[1]: 消息内容 (JSON)
-- ARGV[2]: 聚合窗口时间 (秒)

local listKey = KEYS[1]
local flagKey = KEYS[2]
local counterKey = KEYS[3]
local value = ARGV[1]
local expire = tonumber(ARGV[2])

-- 1. 全局计数器 +1 (记录真实总数)
local count = redis.call('INCR', counterKey)
redis.call('EXPIRE', counterKey, expire * 2)

-- 2. 列表限流 (只存前 100 条详情，超过的直接丢弃详情，只记数)
-- 这样即使有 20万条数据，Redis List 也只有 100 条，避免内存爆炸
if count <= 100 then
    redis.call('RPUSH', listKey, value)
    redis.call('EXPIRE', listKey, expire * 2)
end

-- 3. 检查是否是首条消息 (抢占 Flag)
if redis.call('SETNX', flagKey, '1') == 1 then
    -- 是首条：设置 Flag 过期时间 (等于聚合窗口)
    redis.call('EXPIRE', flagKey, expire)
    return 1 -- 返回 1，表示需要发送 MQ 延迟消息触发器
else
    return 0 -- 返回 0，表示只需存储，无需发送 MQ
end
