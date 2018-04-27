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
    new PublisherClient(settingsFromConfig(config))

  def create(config: PubSubConfig, mat: Materializer, ec: ExecutionContext): JavaPublisherClient = {
    val scalaPublisher = GrpcPublisher(config)(mat, ec)
    JavaPublisherClient.create(
      scalaPublisher.channel,
      settingsFromConfig(config).options.getOrElse(CallOptions.DEFAULT),
      mat,
      ec
    )
  }
}

private[grpc] object GrpcSubscriber {
  import GrpcApi._

  def apply(config: PubSubConfig)(implicit mat: Materializer, ec: ExecutionContext): SubscriberClient =
    new SubscriberClient(settingsFromConfig(config))

  def create(config: PubSubConfig, mat: Materializer, ec: ExecutionContext): JavaSubscriberClient = {
    val scalaSubscriber = GrpcSubscriber(config)(mat, ec)
    JavaSubscriberClient.create(
      scalaSubscriber.channel,
      settingsFromConfig(config).options.getOrElse(CallOptions.DEFAULT),
      mat,
      ec
    )
  }
}

object GrpcApi {
  def settingsFromConfig(config: PubSubConfig) = new GrpcClientSettings(
    config.host,
    config.port,
    options = config.callCredentials.map(CallOptions.DEFAULT.withCallCredentials),
    certificate = config.rootCa
  )

  def subscriptionFqrn(projectId: String, subscriptionName: String) =
    s"projects/$projectId/subscriptions/$subscriptionName"
}
