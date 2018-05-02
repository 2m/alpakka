/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc

import akka.stream.alpakka.googlecloud.pubsub.grpc.impl.GrpcCredentials
import io.grpc.CallCredentials

import scala.util.Try

final class PubSubConfig private (
    val host: String = "pubsub.googleapis.com",
    val port: Int = 443,
    val rootCa: Option[String] = Some("GoogleInternetAuthorityG3.crt"),
    val callCredentials: Option[CallCredentials] = Try(GrpcCredentials.applicationDefault()).toOption,
    val returnImmediately: Boolean = true,
    val maxMessages: Int = 1000
) {

  def withHost(host: String): PubSubConfig = copy(host = host)

  def withPort(port: Int): PubSubConfig = copy(port = port)

  def withRootCa(rootCa: String): PubSubConfig = copy(rootCa = Some(rootCa))
  def withoutRootCa(): PubSubConfig = copy(rootCa = None)

  def withCallCredentials(callCredentials: CallCredentials): PubSubConfig =
    copy(callCredentials = Some(callCredentials))
  def withoutCallCredentials(): PubSubConfig = copy(callCredentials = None)

  private def copy(host: String = host,
                   port: Int = port,
                   rootCa: Option[String] = rootCa,
                   callCredentials: Option[CallCredentials] = callCredentials) =
    new PubSubConfig(host, port, rootCa, callCredentials)
}

object PubSubConfig {
  def apply(): PubSubConfig = new PubSubConfig()
  def create(): PubSubConfig = new PubSubConfig()
}
