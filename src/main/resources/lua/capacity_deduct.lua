-- 选课容量原子预扣减（V1.2 第 1 道防线）
-- KEYS[1] 教学班已选人数计数 Key
-- ARGV[1] capacity         教学班容量
-- ARGV[2] dbCount          数据库权威已选人数（用于冷启动与漂移校正）
-- ARGV[3] ttlSeconds       Key 过期时间
-- 返回 {code, count, capacity}
--   code =  1  扣减成功，count 为扣减后的计数
--   code =  0  名额已满（不扣减）
--   code = -1  计数漂移，已用 dbCount 覆盖校正，本次不扣减（调用方回落到 DB 判定）
local key        = KEYS[1]
local capacity   = tonumber(ARGV[1])
local dbCount    = tonumber(ARGV[2])
local ttl        = tonumber(ARGV[3])

local current = tonumber(redis.call('GET', key))
if current == nil then
    current = dbCount
    redis.call('SET', key, current, 'EX', ttl)
elseif current < 0 or current > capacity then
    redis.call('SET', key, dbCount, 'EX', ttl)
    return { -1, dbCount, capacity }
end

if current >= capacity then
    return { 0, current, capacity }
end

local after = redis.call('INCR', key)
redis.call('EXPIRE', key, ttl)
if after > capacity then
    redis.call('DECR', key)
    return { 0, capacity, capacity }
end
return { 1, after, capacity }
