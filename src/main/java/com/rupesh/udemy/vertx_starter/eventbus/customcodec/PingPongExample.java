package com.rupesh.udemy.vertx_starter.eventbus.customcodec;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongExample {

  private static final Logger LOG = LoggerFactory.getLogger(PingPongExample.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new PingVerticle(), logOnError());
    vertx.deployVerticle(new PongVerticle(), logOnError());
  }

  private static Handler<AsyncResult<String>> logOnError() {
    return ar -> {
      if (ar.failed()) {
        LOG.error("err", ar.cause());
      }
    };
  }

  public static class PingVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(PingVerticle.class);
    static final String ADDRESS = PingVerticle.class.getName();

    @Override
    public void start(final Promise<Void> startPromise) {
      EventBus eventBus = vertx.eventBus();
      final Ping message = new Ping("Hello", true);
      LOG.debug("Sending: {}", message);
      // Register only once
      eventBus.registerDefaultCodec(Ping.class, new LocalMessageCodec<>(Ping.class));
      eventBus.<Pong>request(ADDRESS, message, reply -> {
        if (reply.failed()) {
          LOG.error("Failed: ", reply.cause());
          return;
        }
        LOG.debug("Response: {}", reply.result().body());
      });
      startPromise.complete();
    }
  }

  public static class PongVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(PongVerticle.class);

    @Override
    public void start(final Promise<Void> startPromise) {
      // Register only once
      vertx.eventBus().registerDefaultCodec(Pong.class, new LocalMessageCodec<>(Pong.class));
      vertx.eventBus().<Ping>consumer(PingVerticle.ADDRESS, message -> {
        LOG.debug("Received Message: {}", message.body());
        message.reply(new Pong(0));
      }).exceptionHandler(error -> {
        LOG.error("Error: ", error);
      });
      startPromise.complete();
    }
  }
}
