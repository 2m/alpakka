/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.scaladsl

import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.{Done, NotUsed}
import com.google.pubsub.v1.pubsub._
import io.grpc.CallCredentials

import scala.collection.immutable
import scala.concurrent.Future

object GooglePubSub extends GooglePubSub {
  private[pubsub] override val grpcApi = GrpcApi
}

protected[pubsub] trait GooglePubSub {
  private[pubsub] def grpcApi: GrpcApi

  /**
   * Creates a flow to that publish messages to a topic and emits the message ids
   */
  def publish(creds: CallCredentials, parallelism: Int = 1)(
      implicit materializer: Materializer
  ): Flow[PublishRequest, immutable.Seq[String], NotUsed] = {
    import materializer.executionContext

    Flow[PublishRequest]
      .mapAsyncUnordered(parallelism)(grpcApi.publisher(creds).publish)
      .map(_.messageIds.toVector)
  }

  def subscribe(subscription: String, creds: CallCredentials)(
      implicit materializer: Materializer
  ): Source[ReceivedMessage, NotUsed] = {
    import materializer.executionContext

    val req = StreamingPullRequest(subscription = subscription)
    grpcApi
      .subscriber(creds)
      .streamingPull(Source.single(req))
      .mapConcat(_.receivedMessages.toVector)
      .mapMaterializedValue(_ => NotUsed)
  }

  def acknowledge(creds: CallCredentials,
                  parallelism: Int = 1)(implicit materializer: Materializer): Sink[AcknowledgeRequest, Future[Done]] = {
    import materializer.executionContext

    Flow[AcknowledgeRequest]
      .mapAsyncUnordered(parallelism)(grpcApi.subscriber(creds).acknowledge)
      .toMat(Sink.ignore)(Keep.right)
  }
}
