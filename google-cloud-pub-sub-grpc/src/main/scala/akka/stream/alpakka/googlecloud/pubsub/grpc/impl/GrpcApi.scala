/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc.impl

import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig
import com.google.pubsub.v1.pubsub.{PublisherClient, SubscriberClient}
import com.google.pubsub.v1.{PublisherClient => JavaPublisherClient, SubscriberClient => JavaSubscriberClient}
import io.grpc.CallOptions

import scala.concurrent.ExecutionContext

private[grpc] object GrpcPublisher {
  import GrpcApi._

  def apply(config: PubSubConfig)(implicit mat: Materializer, ec: ExecutionContext): PublisherClient =
    PublisherClient(settingsFromConfig(config))

  def create(config: PubSubConfig, mat: Materializer, ec: ExecutionContext): JavaPublisherClient =
    JavaPublisherClient.create(settingsFromConfig(config), mat, ec)
}

private[grpc] object GrpcSubscriber {
  import GrpcApi._

  def apply(config: PubSubConfig)(implicit mat: Materializer, ec: ExecutionContext): SubscriberClient =
    SubscriberClient(settingsFromConfig(config))

  def create(config: PubSubConfig, mat: Materializer, ec: ExecutionContext): JavaSubscriberClient =
    JavaSubscriberClient.create(settingsFromConfig(config), mat, ec)
}

object GrpcApi {
  def settingsFromConfig(config: PubSubConfig) =
    GrpcClientSettings(
      config.host,
      config.port,
      None,
      config.callCredentials.map(CallOptions.DEFAULT.withCallCredentials),
      config.rootCa
    )
}
