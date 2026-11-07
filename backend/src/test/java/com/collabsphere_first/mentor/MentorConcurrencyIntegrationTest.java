package com.collabsphere_first.mentor;

import com.collabsphere_first.mentor.dto.BookMentorSlotRequest;
import com.collabsphere_first.mentor.entity.MentorSession;
import com.collabsphere_first.mentor.entity.MentorSlot;
import com.collabsphere_first.mentor.repository.MentorProfileRepository;
import com.collabsphere_first.mentor.repository.MentorSessionRepository;
import com.collabsphere_first.mentor.repository.MentorSlotRepository;
import com.collabsphere_first.mentor.service.impl.MentorServiceImpl;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mentor Slot Double-Booking Concurrency Test")
public class MentorConcurrencyIntegrationTest {

    @Mock
    private MentorProfileRepository profileRepository;
    @Mock
    private MentorSlotRepository slotRepository;
    @Mock
    private MentorSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MentorServiceImpl mentorService;

    private UUID slotId;
    private Long mentorId;

    @BeforeEach
    void setUp() {
        slotId = UUID.randomUUID();
        mentorId = 101L;
    }

    @Test
    @DisplayName("Simultaneous booking attempts on the same slot result in exactly 1 success and N-1 race rejections")
    void testConcurrentSlotBookingRaceCondition() throws InterruptedException, ExecutionException {
        MentorSlot slot = MentorSlot.builder()
                .id(slotId)
                .mentorId(mentorId)
                .slotTime(Instant.now().plusSeconds(3600))
                .durationMinutes(30)
                .booked(false)
                .build();

        when(slotRepository.findById(slotId)).thenAnswer(i -> Optional.of(slot));

        // Atomic conditional update simulation: exactly the first thread gets 1, all subsequent threads get 0
        AtomicBoolean isBooked = new AtomicBoolean(false);
        when(slotRepository.reserveSlotAtomically(eq(slotId), any(Instant.class))).thenAnswer(i -> isBooked.compareAndSet(false, true) ? 1 : 0);

        when(sessionRepository.save(any(MentorSession.class))).thenAnswer(i -> {
            MentorSession s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final long learnerId = 500L + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Guarantee true simultaneous execution
                    BookMentorSlotRequest req = BookMentorSlotRequest.builder()
                            .sessionType("System Design")
                            .note("Concurrent booking test")
                            .build();

                    mentorService.bookSlot(slotId, req, learnerId);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage() != null && e.getMessage().contains("already been booked")) {
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(e);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all threads simultaneously
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(numThreads - 1);
    }
}
