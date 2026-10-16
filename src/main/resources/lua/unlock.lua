-- 释放分布式任务锁：校验 value 一致后才删除，避免误删其他实例持有的锁
-- KEYS[1] lock:task:{taskCode}
-- ARGV[1] 加锁时写入的 value
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
