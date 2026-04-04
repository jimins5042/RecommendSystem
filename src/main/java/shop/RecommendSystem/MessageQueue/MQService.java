package shop.RecommendSystem.MessageQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class MQService {

    private final RedisTemplate<String, Object> redisTemplate;

    // redis stream(메시지큐)에 요청 적재
    public boolean sendToStream(String streamName, Map req) {

        try{
            StreamOperations<String, String, Object> streamOps = redisTemplate.opsForStream();
            streamOps.add(streamName, req);
            return true;
        }catch (Exception e){
            log.info("stream에 데이터 적재 중 에러");
            e.printStackTrace();
        }
        return false;
    }

}
