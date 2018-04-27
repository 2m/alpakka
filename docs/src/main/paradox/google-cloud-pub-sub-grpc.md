# Google Cloud Pub/Sub gRPC

The google cloud pub/sub connector provides a way to connect to google clouds managed pub/sub https://cloud.google.com/pubsub/.

This connector communicates to PubSub via GRPC protocol.

### Reported issues

[Tagged issues at Github](https://github.com/akka/alpakka/labels/p%3Agoogle-cloud-pub-sub-grpc)


## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=com.lightbend.akka
  artifact=akka-stream-alpakka-google-cloud-pub-sub-grpc_$scalaBinaryVersion$
  version=$version$
}

## Usage

Credentials are automatically collected from your `GOOGLE_APPLICATION_CREDENTIAL` environment variable. Please check
[Google official documentation](https://cloud.google.com/pubsub/docs/reference/libraries#setting_up_authentication) for more details.

Prepare the actor system and materializer:

Scala
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/scala/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsage.scala) { #init-mat }

Java
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/java/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsageJava.java) { #init-mat }

### Client configuration

We need to configure the client which will connect using GRPC.

Scala
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/scala/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsage.scala) { #init-client }

Java
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/java/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsageJava.java) { #init-client }

The option `usePlaintext` is useful when used in conjunction with [PubSub emulator](https://cloud.google.com/pubsub/docs/emulator).
`returnImmediately` and `maxMessages` match the spec as given in the [Google PubSub documentation](https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions/pull).

The parallelism parameter allows more than a single in-flight request. See [GitHub PR #759](https://github.com/akka/alpakka/pull/759) for more details.

The `retryOnFailure` and `maxConsecutiveErrors` are used to re-establish connection to PubSub if the connection is terminated by PubSub. Note retrying to connect is implemented using exponential back0off.

### Publishing 

We first construct a message and then a request using Google's builders. We declare a singleton source which will go via our publishing flow. All messages sent to the flow are published to PubSub.

Scala
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/scala/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsage.scala) { #publish-single }

Java
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/java/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsageJava.java) { #publish-single }


Similarly to before, we can publish a batch of messages for greater efficiency.

Scala
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/scala/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsage.scala) { #publish-fast }

Java
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/java/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsageJava.java) { #publish-fast }

Finally, to automatically acknowledge messages and send messages to your own sink, once can do the following:

Scala
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/scala/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsage.scala) { #subscribe-auto-ack }

Java
: @@snip ($alpakka$/google-cloud-pub-sub-grpc/src/test/java/akka/stream/alpakka/googlecloud/pubsub/grpc/ExampleUsageJava.java) { #subscribe-auto-ack }

## Running the examples

To run the example code you will need to configure a project and pub/sub in google cloud and provide your own credentials.
