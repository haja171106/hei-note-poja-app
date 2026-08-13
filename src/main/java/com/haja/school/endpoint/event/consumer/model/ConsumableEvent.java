package com.haja.school.endpoint.event.consumer.model;

import com.haja.school.PojaGenerated;
import lombok.AllArgsConstructor;
import lombok.Getter;

@PojaGenerated
@AllArgsConstructor
public class ConsumableEvent {
  @Getter private final TypedEvent event;
  private final Runnable acknowledger;
  private final Runnable randomVisibilityTimeoutSetter;

  public void ack() {
    acknowledger.run();
  }

  public void newRandomVisibilityTimeout() {
    randomVisibilityTimeoutSetter.run();
  }
}
