package com.haja.school.service.event;

import static org.mockito.Mockito.verify;

import com.haja.school.endpoint.event.model.DurablyFallibleUuidCreated1;
import com.haja.school.endpoint.event.model.DurablyFallibleUuidCreated2;
import com.haja.school.endpoint.event.model.UuidCreated;
import com.haja.school.repository.DummyUuidRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UuidCreatedServiceTest {

  @Mock private DummyUuidRepository repository;

  @Test
  void consumesUuidCreatedEvent() {
    UuidCreatedService service = new UuidCreatedService(repository);
    String uuid = UUID.randomUUID().toString();

    service.accept(UuidCreated.builder().uuid(uuid).build());

    verify(repository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                (com.haja.school.repository.model.DummyUuid value) -> uuid.equals(value.getId())));
  }

  @Test
  void durableConsumersDelegateSuccessfulEvents() {
    UuidCreatedService delegate = org.mockito.Mockito.mock(UuidCreatedService.class);
    UuidCreated event = UuidCreated.builder().uuid(UUID.randomUUID().toString()).build();

    new DurablyFallibleUuidCreated1Service(delegate)
        .accept(
            DurablyFallibleUuidCreated1.builder()
                .uuidCreated(event)
                .waitDurationBeforeConsumingInSeconds(0)
                .failureRate(0)
                .build());
    new DurablyFallibleUuidCreated2Service(delegate)
        .accept(
            DurablyFallibleUuidCreated2.builder()
                .uuidCreated(event)
                .waitDurationBeforeConsumingInSeconds(0)
                .failureRate(0)
                .build());

    verify(delegate, org.mockito.Mockito.times(2)).accept(event);
  }

  @Test
  void durableConsumerFailsWithoutDelegatingWhenFailureRateIsCertain() {
    UuidCreatedService delegate = org.mockito.Mockito.mock(UuidCreatedService.class);
    UuidCreated event = UuidCreated.builder().uuid(UUID.randomUUID().toString()).build();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new DurablyFallibleUuidCreated1Service(delegate)
                    .accept(
                        DurablyFallibleUuidCreated1.builder()
                            .uuidCreated(event)
                            .waitDurationBeforeConsumingInSeconds(0)
                            .failureRate(1)
                            .build()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Oops, random fail!");

    org.mockito.Mockito.verifyNoInteractions(delegate);
  }
}
