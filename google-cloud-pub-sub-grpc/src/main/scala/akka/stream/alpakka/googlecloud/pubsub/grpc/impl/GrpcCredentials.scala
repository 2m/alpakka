/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.grpc.impl

import com.google.auth.oauth2.GoogleCredentials
import io.grpc.CallCredentials
import io.grpc.auth.MoreCallCredentials

import scala.collection.JavaConverters._

private[grpc] object GrpcCredentials {

  def applicationDefault(): CallCredentials =
    MoreCallCredentials.from(
      GoogleCredentials.getApplicationDefault.createScoped(List("https://www.googleapis.com/auth/pubsub").asJava)
    )

}
