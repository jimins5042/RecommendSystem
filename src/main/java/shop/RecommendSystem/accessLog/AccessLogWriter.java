package shop.RecommendSystem.accessLog;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;
import shop.RecommendSystem.dto.AccessLog;
import shop.RecommendSystem.repository.mapper.AccessLogMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 접근 로그를 큐에 모아 별도 스레드에서 배치 INSERT 한다.
 *
 * <p>설계 원칙은 하나다 — <b>로그 때문에 요청이 느려지거나 실패하면 안 된다.</b>
 * <ul>
 *   <li>요청 스레드는 {@link #enqueue} 로 큐에 넣고 즉시 반환한다(블로킹 없음)</li>
 *   <li>큐가 가득 차면 <b>버린다</b>. 대기하지 않는다</li>
 *   <li>DB 적재가 실패해도 해당 배치만 포기하고 다음으로 넘어간다</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogWriter {

    private static final int QUEUE_CAPACITY = 10_000;
    private static final int BATCH_SIZE = 100;
    private static final long POLL_TIMEOUT_MS = 3_000L;
    private static final long SHUTDOWN_WAIT_MS = 5_000L;

    private final SqlSessionFactory sqlSessionFactory;

    private final BlockingQueue<AccessLog> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong droppedCount = new AtomicLong();

    private volatile boolean running = true;
    private Thread worker;

    @PostConstruct
    void start() {

        worker = new Thread(this::consume, "access-log-writer");
        worker.setDaemon(true);
        worker.start();

        log.info("접근 로그 기록 시작 (큐 {}건, 배치 {}건)", QUEUE_CAPACITY, BATCH_SIZE);
    }

    @PreDestroy
    void stop() {

        running = false;

        if (worker != null) {
            try {
                // 남은 큐를 비울 시간을 준다
                worker.join(SHUTDOWN_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long dropped = droppedCount.get();
        if (dropped > 0) {
            log.warn("접근 로그 {}건이 큐 포화로 누락됨", dropped);
        }
    }

    /**
     * 요청 스레드에서 호출된다. 절대 블로킹하거나 예외를 던지지 않는다.
     */
    public void enqueue(AccessLog accessLog) {

        if (queue.offer(accessLog)) {
            return;
        }

        // 큐 포화 — 요청을 지연시키느니 로그를 버린다. 로그 폭주를 막으려 1000건마다 한 번만 경고
        long dropped = droppedCount.incrementAndGet();
        if (dropped % 1000 == 1) {
            log.warn("접근 로그 큐 포화로 누락 중 (누적 {}건). DB 적재가 밀리고 있는지 확인 필요", dropped);
        }
    }

    private void consume() {

        List<AccessLog> batch = new ArrayList<>(BATCH_SIZE);

        // 종료 신호를 받아도 큐가 빌 때까지는 계속 적재한다
        while (running || !queue.isEmpty()) {
            try {
                AccessLog head = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (head == null) {
                    continue;
                }

                batch.add(head);
                queue.drainTo(batch, BATCH_SIZE - 1);

                flush(batch);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // DB 장애 등 — 이 배치만 포기한다
                log.warn("접근 로그 {}건 적재 실패: {}", batch.size(), e.toString());
            } finally {
                batch.clear();
            }
        }
    }

    private void flush(List<AccessLog> batch) {

        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {

            AccessLogMapper mapper = session.getMapper(AccessLogMapper.class);
            for (AccessLog accessLog : batch) {
                mapper.insert(accessLog);
            }
            session.commit();
        }
    }
}
