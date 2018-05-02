/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.scaladsl.GooglePubSub
import akka.stream.scaladsl.{Sink, Source}
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub.{AcknowledgeRequest, PublishRequest, PubsubMessage, StreamingPullRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}

import scala.concurrent.duration._

class IntegrationSpec extends WordSpec with Matchers with Inside with BeforeAndAfterAll with ScalaFutures {

  implicit val sys = ActorSystem("IntegrationSpec")
  implicit val mat = ActorMaterializer()

  implicit val defaultPatience = PatienceConfig(timeout = 15.seconds, interval = 50.millis)

  val config = PubSubConfig().withHost("localhost").withPort(8538).withoutRootCa().withoutCallCredentials()

  val projectId = "alpakka"
  val topic = "testTopic"
  val subscription = "testSubscription"

  val topicFqrs = s"projects/$projectId/topics/$topic"
  val subscriptionFqrs = s"projects/$projectId/subscriptions/$subscription"

  "connector" should {

    "publish receive and ack" in {
      val msg = "Labas!"

      val pub = PublishRequest(topicFqrs, Seq(PubsubMessage(ByteString.copyFromUtf8(msg))))
      val pubResp = Source.single(pub).via(GooglePubSub.publish(config, parallelism = 1)).runWith(Sink.head)

      pubResp.futureValue.messageIds should not be empty

      val sub = StreamingPullRequest(subscriptionFqrs, streamAckDeadlineSeconds = 10)
      val subNoAckResp = GooglePubSub.subscribe(config, sub).runWith(Sink.head)

      inside(subNoAckResp.futureValue.message) {
        case Some(PubsubMessage(data, _, _, _)) => data.toStringUtf8 shouldBe msg
      }

      val ack = GooglePubSub
        .subscribe(config, sub)
        .take(1)
        .map(msg => AcknowledgeRequest(subscriptionFqrs, Seq(msg.ackId)))
        .runWith(GooglePubSub.acknowledge(config, parallelism = 1))

      ack.futureValue

      GooglePubSub
        .subscribe(config, sub)
        .idleTimeout(15.seconds)
        .runWith(Sink.ignore)
        .failed
        .futureValue
    }

  }

  override def afterAll() =
    sys.terminate()

}
