-- 退课 / 递补 / 事务回滚的容量回补与补偿
-- KEYS[1] 教学班已选人数计数 Key
-- ARGV[1] dbCount      数据库权威已选人数（回补下界）
-- ARGV[2] ttlSeconds   Key 过期时间
-- 返回 {code, count}
--   code = 1 回补完成（count 为回补后的计数）
local key     = KEYS[1]
local dbCount = tonumber(ARGV[1])
local ttl     = tonumber(ARGV[2])

local current = tonumber(redis.call('GET', key))
if current == nil or current <= 0 then
    redis.call('SET', key, dbCount, 'EX', ttl)
    return { 1, dbCount }
end

local after = redis.call('DECR', key)
redis.call('EXPIRE', key, ttl)
if after < dbCount then
    -- 回补过度（历史计数偏低）时以 DB 为准，保证计数永不低于真实值
    redis.call('SET', key, dbCount, 'EX', ttl)
    after = dbCount
end
return { 1, after }
