/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc.javadsl

import java.util.concurrent.CompletionStage

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig
import akka.stream.alpakka.googlecloud.pubsub.grpc.impl.{GrpcApi, GrpcPublisher, GrpcSubscriber}
import akka.stream.javadsl.{Flow, Keep, Sink, Source}
import com.google.pubsub.v1._

object GooglePubSub {

  def publish(config: PubSubConfig,
              parallelism: Int,
              materializer: Materializer): Flow[PublishRequest, PublishResponse, NotUsed] = {
    val publisher = GrpcPublisher.create(config, materializer, materializer.executionContext)
    Flow
      .create[PublishRequest]()
      .mapAsyncUnordered(parallelism, publisher.publish)
  }

  def subscribe(config: PubSubConfig,
                projectId: String,
                subscriptionName: String,
                materializer: Materializer): Source[ReceivedMessage, NotUsed] = {
    val req = StreamingPullRequest
      .newBuilder()
      .setSubscription(GrpcApi.subscriptionFqrn(projectId, subscriptionName))
      .build()
    GrpcSubscriber
      .create(config, materializer, materializer.executionContext)
      .streamingPull(Source.single(req))
      .mapConcat(_.getReceivedMessagesList)
  }

  def acknowledge(config: PubSubConfig,
                  parallelism: Int,
                  materializer: Materializer): Sink[AcknowledgeRequest, CompletionStage[Done]] = {
    val subscriber = GrpcSubscriber.create(config, materializer, materializer.executionContext)
    Flow
      .create[AcknowledgeRequest]
      .mapAsyncUnordered(parallelism, subscriber.acknowledge)
      .toMat(Sink.ignore, Keep.right[akka.NotUsed, CompletionStage[Done]])
  }

}
