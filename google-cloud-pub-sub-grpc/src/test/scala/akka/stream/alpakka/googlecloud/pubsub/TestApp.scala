/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.googlecloud.pubsub.scaladsl.GooglePubSub
import akka.stream.scaladsl.Source
import com.google.auth.oauth2.GoogleCredentials
import com.google.protobuf.ByteString
import com.google.pubsub.v1.pubsub.{PublishRequest, PubsubMessage}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

object TestApp {
  def main(args: Array[String]): Unit = {
    implicit val sys = ActorSystem()
    implicit val mat = ActorMaterializer()

    try {
      run()
      scala.io.StdIn.readLine()
    } finally {
      Await.ready(sys.terminate(), 5.seconds)
    }
  }

  def run()(implicit sys: ActorSystem, mat: Materializer): Unit = {
    val requests = Source(
      immutable.Seq(
        PublishRequest(
          "projects/kraziu-tvirtove/topics/testTopic",
          Seq(
            PubsubMessage(ByteString.copyFromUtf8("hi")),
            PubsubMessage(ByteString.copyFromUtf8("my name is")),
            PubsubMessage(ByteString.copyFromUtf8("who"))
          )
        )
      )
    )

    // Loads credentials from a json file, which is found
    // from GOOGLE_APPLICATION_CREDENTIALS env variable
    val creds = GoogleCredentials.getApplicationDefault

    requests.via(GooglePubSub.publish(creds)).runForeach(println)
  }
}
