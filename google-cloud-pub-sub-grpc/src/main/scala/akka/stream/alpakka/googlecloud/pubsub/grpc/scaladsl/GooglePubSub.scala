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
  ): Flow[PublishRequest, PublishResponse, NotUsed] = {
    import materializer.executionContext

    val publisher = GrpcPublisher(config)
    Flow[PublishRequest]
      .mapAsyncUnordered(parallelism)(publisher.publish)
  }

  def subscribe(config: PubSubConfig, projectId: String, subscriptionName: String)(
      implicit materializer: Materializer
  ): Source[ReceivedMessage, NotUsed] = {
    import materializer.executionContext

    val req = StreamingPullRequest(GrpcApi.subscriptionFqrn(projectId, subscriptionName))
    GrpcSubscriber(config)
      .streamingPull(Source.single(req))
      .mapConcat(_.receivedMessages.toVector)
      .mapMaterializedValue(_ => NotUsed)
  }

  def acknowledge(config: PubSubConfig,
                  parallelism: Int)(implicit materializer: Materializer): Sink[AcknowledgeRequest, Future[Done]] = {
    import materializer.executionContext

    val subscriber = GrpcSubscriber(config)
    Flow[AcknowledgeRequest]
      .mapAsyncUnordered(parallelism)(subscriber.acknowledge)
      .toMat(Sink.ignore)(Keep.right)
  }
}
