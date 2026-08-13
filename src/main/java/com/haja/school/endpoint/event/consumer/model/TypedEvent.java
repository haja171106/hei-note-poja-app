package com.haja.school.endpoint.event.consumer.model;

import com.haja.school.PojaGenerated;
import com.haja.school.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
