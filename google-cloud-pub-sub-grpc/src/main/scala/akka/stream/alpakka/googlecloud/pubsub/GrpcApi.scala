/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub

import akka.stream.Materializer
import com.google.pubsub.v1.pubsub.{PublisherClient, SubscriberClient}
import io.grpc.{CallCredentials, CallOptions, ManagedChannelBuilder}

import scala.concurrent.ExecutionContext

private object GrpcApi extends GrpcApi {
  final val DefaultPubSubGoogleApisHost = "pubsub.googleapis.com"
}

private trait GrpcApi {
  import GrpcApi._

  private val channel = ManagedChannelBuilder.forTarget(DefaultPubSubGoogleApisHost).build()

  def publisher(credentials: CallCredentials)(implicit mat: Materializer, ec: ExecutionContext) =
    new PublisherClient(channel,
                        CallOptions.DEFAULT
                          .withCallCredentials(credentials))

  def subscriber(credentials: CallCredentials)(implicit mat: Materializer, ec: ExecutionContext) =
    new SubscriberClient(channel,
                         CallOptions.DEFAULT
                           .withCallCredentials(credentials))
}
