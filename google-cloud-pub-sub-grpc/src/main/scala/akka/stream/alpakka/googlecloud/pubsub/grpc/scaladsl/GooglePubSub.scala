/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc.scaladsl

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig
import akka.stream.alpakka.googlecloud.pubsub.grpc.impl.{GrpcApi, GrpcPublisher, GrpcSubscriber}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.google.pubsub.v1.pubsub._

import scala.concurrent.Future

object GooglePubSub {

  def publish(config: PubSubConfig, parallelism: Int)(
      implicit materializer: Materializer
  ): Flow[PublishRequest, PublishResponse, NotUsed] =
    Flow.lazyInit(
      _ => {
        import materializer.executionContext
        val publisher = GrpcPublisher(config)
        val flow = Flow[PublishRequest].mapAsyncUnordered(parallelism)(publisher.publish).watchTermination() {
          (_, completion) =>
            completion.onComplete(_ => publisher.close())
            NotUsed
        }
        Future.successful(flow)
      },
      () => NotUsed
    )

  def subscribe(config: PubSubConfig, request: StreamingPullRequest)(
      implicit materializer: Materializer
  ): Source[ReceivedMessage, NotUsed] =
    Source
      .lazily { () =>
        import materializer.executionContext
        val subscriber = GrpcSubscriber(config)
        subscriber
          .streamingPull(Source.single(request).concat(Source.maybe))
          .mapConcat(_.receivedMessages.toVector)
          .watchTermination() { (_, completion) =>
            completion.onComplete(_ => subscriber.close())
            NotUsed
          }
      }
      .mapMaterializedValue(_ => NotUsed)

  def acknowledge(config: PubSubConfig,
                  parallelism: Int)(implicit materializer: Materializer): Sink[AcknowledgeRequest, Future[Done]] = {
    import materializer.executionContext

    Sink
      .lazyInit(
        (_: AcknowledgeRequest) => {
          val subscriber = GrpcSubscriber(config)
          val sink = Flow[AcknowledgeRequest]
            .mapAsyncUnordered(parallelism)(subscriber.acknowledge)
            .watchTermination() { (_, completion) =>
              completion.onComplete(_ => subscriber.close())
              NotUsed
            }
            .toMat(Sink.ignore)(Keep.right)
          Future.successful(sink)
        },
        () => Future.successful(Done)
      )
      .mapMaterializedValue(_.flatten)
  }
}
