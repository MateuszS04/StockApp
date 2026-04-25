--trade.lua
--Atomically transfers one unit of stock from a source hash to destination hash.a
--
--KEYS[1]=source hash ("bank:stocks" fro buy, "wallet:w1:stocks" for sell)
--KEYS[2]=destination hash ("wallet:w1:stocks" for buy, "bank:stocks" fro sell)
--ARGV[1]=stock name

--returns :
-- 1=success (transfer done)
-- 0=failure

local available= redis.call('HGET',KEYS[1], ARGV[1])

if not available or tonumber(available)<1 then
    return 0
end

redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
redis.call('HINCRBY',KEYS[2], ARGV[1],1)
return 1
