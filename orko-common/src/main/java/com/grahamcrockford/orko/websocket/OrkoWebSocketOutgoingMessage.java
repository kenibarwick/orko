package com.grahamcrockford.orko.websocket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize
abstract class OrkoWebSocketOutgoingMessage {

  @JsonCreator
  static OrkoWebSocketOutgoingMessage create(@JsonProperty("nature") Nature nature,
                                            @JsonProperty("data") Object data) {
    return new AutoValue_OrkoWebSocketOutgoingMessage(nature, data);
  }

  @JsonProperty
  abstract Nature nature();

  @JsonProperty
  abstract Object data();

  enum Nature {
    ERROR,
    TICKER,
    OPEN_ORDERS,
    ORDERBOOK,
    USER_TRADE,
    TRADE,
    USER_TRADE_HISTORY,
    BALANCE,
    NOTIFICATION,
    STATUS_UPDATE
  }
}