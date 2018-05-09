/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc.javadsl

import java.util.concurrent.{CompletableFuture, CompletionStage}

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig
import akka.stream.alpakka.googlecloud.pubsub.grpc.impl.{GrpcPublisher, GrpcSubscriber}
import akka.stream.javadsl.{Flow, Keep, Sink, Source}
import com.google.pubsub.v1._

object GooglePubSub {

  def publish(config: PubSubConfig,
              parallelism: Int,
              materializer: Materializer): Flow[PublishRequest, PublishResponse, NotUsed] =
    Flow.lazyInit(
      (_: PublishRequest) => {
        // FIXME call close() on publisher when it gets one
        val publisher = GrpcPublisher.create(config, materializer, materializer.executionContext)
        val flow = Flow
          .create[PublishRequest]()
          .mapAsyncUnordered(parallelism, publisher.publish)
        CompletableFuture.completedFuture(flow)
      },
      () => NotUsed
    )

  def subscribe(config: PubSubConfig,
                request: StreamingPullRequest,
                materializer: Materializer): Source[ReceivedMessage, NotUsed] =
    Source
      .lazily { () =>
        // FIXME call close() on subscriber when it gets one
        GrpcSubscriber
          .create(config, materializer, materializer.executionContext)
          .streamingPull(Source.single(request))
          .mapConcat(_.getReceivedMessagesList)
      }
      .mapMaterializedValue(_ => NotUsed)

  def acknowledge(config: PubSubConfig,
                  parallelism: Int,
                  materializer: Materializer): Sink[AcknowledgeRequest, CompletionStage[Done]] =
    Sink
      .lazyInit(
        (_: AcknowledgeRequest) => {
          // FIXME call close() on subscriber when it gets one
          val subscriber = GrpcSubscriber.create(config, materializer, materializer.executionContext)
          val sink = Flow
            .create[AcknowledgeRequest]
            .mapAsyncUnordered(parallelism, subscriber.acknowledge)
            .toMat(Sink.ignore(), Keep.right[akka.NotUsed, CompletionStage[Done]])
          CompletableFuture.completedFuture(sink)
        },
        () => CompletableFuture.completedFuture(Done.getInstance())
      )
      .mapMaterializedValue(f => f.thenCompose(i => i))
}
