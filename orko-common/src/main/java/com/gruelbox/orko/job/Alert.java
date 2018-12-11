package com.gruelbox.orko.job;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import com.gruelbox.orko.jobrun.spi.Job;
import com.gruelbox.orko.jobrun.spi.JobBuilder;
import com.gruelbox.orko.jobrun.spi.JobControl;
import com.gruelbox.orko.jobrun.spi.JobProcessor;
import com.gruelbox.orko.notification.Notification;

@AutoValue
@JsonDeserialize(builder = Alert.Builder.class)
public abstract class Alert implements Job {

  public static final Builder builder() {
    return new AutoValue_Alert.Builder();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "")
  public abstract static class Builder implements JobBuilder<Alert> {
    @JsonCreator private static Builder create() { return Alert.builder(); }
    @Override
    public abstract Builder id(String value);
    public abstract Builder notification(Notification notification);

    @Override
    public abstract Alert build();
  }

  @Override
  @JsonIgnore
  public abstract Builder toBuilder();

  @Override
  @JsonProperty
  @Nullable
  public abstract String id();

  @JsonProperty
  public abstract Notification notification();

  @Override
  public String toString() {
    return String.format("send " + notification().level().toString().toLowerCase() + " '%s'", notification().message());
  }

  @JsonIgnore
  @Override
  public final Class<Processor.ProcessorFactory> processorFactory() {
    return Processor.ProcessorFactory.class;
  }

  public interface Processor extends JobProcessor<Alert> {
    public interface ProcessorFactory extends JobProcessor.Factory<Alert> {
      @Override
      Processor create(Alert job, JobControl jobControl);
    }
  }
}