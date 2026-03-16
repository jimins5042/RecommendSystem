package shop.RecommendSystem.MessageQueue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
@Slf4j
public class ClassificationNotificationListener implements MessageListener {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {

            //구독중인 redis pub/sub 채널 구분
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

            if (channel == null || channel.isEmpty() || !("imgSearch:preprocess_alert").equals(channel)) {
                return;
            }

            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode notification = objectMapper.readTree(body);

            log.info("RAW Redis notify  : {}", body);

            String requestId = notification.get("request_id").asText();
            String batchId = notification.get("batch_id").asText();
            String status = notification.get("status").asText();

            if (batchId == null || batchId.isEmpty() || requestId == null || requestId.isEmpty()) {
                return;
            }

            // Redis Hash에서 전체 결과 조회
            Object resultObj = redisTemplate.opsForHash().get("imgSearch:results", requestId);

            log.info("RAW Redis message: {}", resultObj);

            if (resultObj == null) {
                log.warn("Result not found for requestId={}", requestId);
                return;
            }

        } catch (Exception e) {
            log.error("Failed to process notification", e);
        }
    }
}
