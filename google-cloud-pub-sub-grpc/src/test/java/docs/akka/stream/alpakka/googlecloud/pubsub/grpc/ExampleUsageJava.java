/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.akka.stream.alpakka.googlecloud.pubsub.grpc;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.googlecloud.pubsub.grpc.PubSubConfig;
import akka.stream.alpakka.googlecloud.pubsub.grpc.javadsl.GooglePubSub;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import com.google.pubsub.v1.StreamingPullRequest;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class ExampleUsageJava {

    private static void example() {

        //#init-mat
        ActorSystem system = ActorSystem.create();
        ActorMaterializer materializer = ActorMaterializer.create(system);
        //#init-mat

        //#init-client
        String projectId = "test-XXXXX";
        String topic = "topic1";
        String subscription = "subscription1";

        PubSubConfig config = PubSubConfig.create();
        //#init-client

        //#publish-single
        PubsubMessage publishMessage =
                PubsubMessage
                        .newBuilder()
                        .setMessageId("1")
                        .setData(ByteString.copyFromUtf8("Hello world!"))
                        .build();

        PublishRequest publishRequest =
                PublishRequest
                        .newBuilder()
                        .addMessages(publishMessage)
                        .build();

        Source<PublishRequest, NotUsed> source = Source.single(publishRequest);

        Flow<PublishRequest, PublishResponse, NotUsed> publishFlow =
          GooglePubSub.publish(config, 1, materializer);

        CompletionStage<List<PublishResponse>> publishedMessageIds =
                source.via(publishFlow).runWith(Sink.seq(), materializer);
        //#publish-single

        //#publish-fast
        Source<PubsubMessage, NotUsed> messageSource = Source.single(publishMessage);
        messageSource.groupedWithin(1000, FiniteDuration.apply(1, "min"))
                .map(messages -> PublishRequest
                        .newBuilder()
                        .setTopic(topic)
                        .addAllMessages(messages)
                        .build())
                .via(publishFlow)
                .runWith(Sink.ignore(), materializer);
        //#publish-fast


        //#subscribe
        StreamingPullRequest request = StreamingPullRequest.newBuilder().setSubscription(subscription).build();

        Source<ReceivedMessage, NotUsed> subscriptionSource =
          GooglePubSub.subscribe(config, request, materializer);

        Sink<AcknowledgeRequest, CompletionStage<Done>> ackSink =
          GooglePubSub.acknowledge(config, 1, materializer);


        subscriptionSource.map(receivedMessage -> {
            // do some computation
            return receivedMessage.getAckId();
        })
                .groupedWithin(1000, FiniteDuration.apply(1, "min"))
                .map(acks -> AcknowledgeRequest.newBuilder().addAllAckIds(acks).build())
                .to(ackSink);
        //#subscribe


        Sink<ReceivedMessage, CompletionStage<Done>> yourProcessingSink = Sink.ignore();

        //#subscribe-auto-ack
        Sink<ReceivedMessage, CompletionStage<Done>> processSink = yourProcessingSink;

        Sink<ReceivedMessage, NotUsed> batchAckSink = Flow.of(ReceivedMessage.class)
                .map(t -> t.getAckId())
                .groupedWithin(1000, FiniteDuration.apply(1, "minute"))
                .map(ids -> AcknowledgeRequest.newBuilder().addAllAckIds(ids).build())
                .to(ackSink);

        subscriptionSource.alsoTo(batchAckSink).to(processSink);
        //#subscribe-auto-ack
    }

}
