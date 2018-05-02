/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.akka.stream.alpakka.googlecloud.pubsub.grpc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig
import akka.stream.alpakka.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._

class ExampleUsage {

  //#init-mat
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  //#init-mat

  //#init-client
  val projectId = "test-XXXXX"
  val topic = "topic1"
  val subscription = "subscription1"

  val config = PubSubConfig()
  //#init-client

  //#publish-single
  val publishMessage: PubsubMessage =
    PubsubMessage()
      .withData(ByteString.copyFromUtf8("Hello world!"))

  val publishRequest: PublishRequest =
    PublishRequest()
      .withTopic(topic)
      .addMessages(publishMessage)

  val source: Source[PublishRequest, NotUsed] =
    Source.single(publishRequest)

  val publishFlow: Flow[PublishRequest, PublishResponse, NotUsed] =
    GooglePubSub.publish(config, parallelism = 1)

  val publishedMessageIds: Future[Seq[PublishResponse]] = source.via(publishFlow).runWith(Sink.seq)
  //#publish-single

  //#publish-fast
  val messageSource: Source[PubsubMessage, NotUsed] = Source(List(publishMessage, publishMessage))
  messageSource
    .groupedWithin(1000, 1.minute)
    .map { msgs =>
      PublishRequest()
        .withTopic(topic)
        .addAllMessages(msgs)
    }
    .via(publishFlow)
    .to(Sink.seq)
  //#publish-fast

  //#subscribe
  val request = StreamingPullRequest(subscription)

  val subscriptionSource: Source[ReceivedMessage, NotUsed] =
    GooglePubSub.subscribe(config, request)

  val ackSink: Sink[AcknowledgeRequest, Future[Done]] =
    GooglePubSub.acknowledge(config, parallelism = 1)

  subscriptionSource
    .map { message =>
      // do something fun
      message.ackId
    }
    .groupedWithin(1000, 1.minute)
    .map(ids => AcknowledgeRequest(ackIds = ids))
    .to(ackSink)
  //#subscribe

  //#subscribe-auto-ack
  val subscribeMessageSource: Source[ReceivedMessage, NotUsed] = ???
  val processMessage: Sink[ReceivedMessage, NotUsed] = ???

  val batchAckSink: Sink[ReceivedMessage, NotUsed] =
    Flow[ReceivedMessage]
      .map(_.ackId)
      .groupedWithin(1000, 1.minute)
      .map(ids => AcknowledgeRequest(ackIds = ids))
      .to(ackSink)

  val q: RunnableGraph[NotUsed] = subscribeMessageSource.alsoTo(batchAckSink).to(processMessage)
  //#subscribe-auto-ack

}
