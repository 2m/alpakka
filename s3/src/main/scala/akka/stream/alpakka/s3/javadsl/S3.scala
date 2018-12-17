/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.s3.javadsl
import java.util.Optional

import akka.japi.{Pair => JPair}
import akka.{Done, NotUsed}
import akka.http.javadsl.model._
import akka.http.javadsl.model.headers.ByteRange
import akka.http.scaladsl.model.headers.{ByteRange => ScalaByteRange}
import akka.http.scaladsl.model.{ContentType => ScalaContentType, HttpMethod => ScalaHttpMethod}
import akka.stream.alpakka.s3.acl.CannedAcl
import akka.stream.alpakka.s3.impl._
import akka.stream.alpakka.s3.scaladsl
import akka.stream.javadsl.{RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.compat.java8.OptionConverters._

object S3 {

  /**
   * Use this to extend the library
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param method the [[akka.http.javadsl.model.HttpMethod HttpMethod]] to use when making the request
   * @param s3Headers any headers you want to add
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the raw [[HttpResponse]]
   */
  def request(bucket: String, key: String, method: HttpMethod, s3Headers: S3Headers): Source[HttpResponse, NotUsed] =
    request(bucket, key, Optional.empty(), method, s3Headers)

  /**
   * Use this to extend the library
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional versionId of source object
   * @param method the [[akka.http.javadsl.model.HttpMethod HttpMethod]] to use when making the request
   * @param s3Headers any headers you want to add
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the raw [[HttpResponse]]
   */
  def request(bucket: String,
              key: String,
              versionId: Optional[String],
              method: HttpMethod = HttpMethods.GET,
              s3Headers: S3Headers = S3Headers.empty): Source[HttpResponse, NotUsed] =
    S3Stream
      .request(S3Location(bucket, key),
               method.asInstanceOf[ScalaHttpMethod],
               versionId = Option(versionId.orElse(null)),
               s3Headers = s3Headers)
      .asJava

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[java.util.concurrent.CompletionStage CompletionStage]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String, key: String): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadata(bucket, key, null)

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return A [[java.util.concurrent.CompletionStage CompletionStage]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String,
                        key: String,
                        sse: ServerSideEncryption): Source[Optional[ObjectMetadata], NotUsed] =
    getObjectMetadata(bucket, key, Optional.empty(), sse)

  /**
   * Gets the metadata for a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional versionId of source object
   * @param sse the server side encryption to use
   * @return A [[java.util.concurrent.CompletionStage CompletionStage]] containing an [[java.util.Optional Optional]] that will be empty in case the object does not exist
   */
  def getObjectMetadata(bucket: String,
                        key: String,
                        versionId: Optional[String],
                        sse: ServerSideEncryption): Source[Optional[ObjectMetadata], NotUsed] =
    S3Stream
      .getObjectMetadata(bucket, key, Option(versionId.orElse(null)), Option(sse))
      .map { opt =>
        Optional.ofNullable(opt.map(metaDataToJava).orNull)
      }
      .asJava

  /**
   * Deletes a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[java.util.concurrent.CompletionStage CompletionStage]] of [[java.lang.Void]]
   */
  def deleteObject(bucket: String, key: String): Source[Done, NotUsed] =
    deleteObject(bucket, key, Optional.empty())

  /**
   * Deletes a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param versionId optional version id of the object
   * @return A [[java.util.concurrent.CompletionStage CompletionStage]] of [[java.lang.Void]]
   */
  def deleteObject(bucket: String, key: String, versionId: Optional[String]): Source[Done, NotUsed] =
    S3Stream
      .deleteObject(S3Location(bucket, key), Option(versionId.orElse(null)))
      .map(_ => Done.getInstance())
      .asJava

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType,
                s3Headers: S3Headers): Source[ObjectMetadata, NotUsed] =
    S3Stream
      .putObject(S3Location(bucket, key),
                 contentType.asInstanceOf[ScalaContentType],
                 data.asScala,
                 contentLength,
                 s3Headers,
                 None)
      .map(metaDataToJava)
      .asJava

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType,
                s3Headers: S3Headers,
                sse: ServerSideEncryption): Source[ObjectMetadata, NotUsed] =
    S3Stream
      .putObject(S3Location(bucket, key),
                 contentType.asInstanceOf[ScalaContentType],
                 data.asScala,
                 contentLength,
                 s3Headers,
                 Some(sse))
      .map(metaDataToJava)
      .asJava

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param cannedAcl the Acl
   * @param metaHeaders the metadata headers
   * @return ta [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType,
                cannedAcl: CannedAcl,
                metaHeaders: MetaHeaders): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, contentType, S3Headers(cannedAcl, metaHeaders))

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param cannedAcl the Acl
   * @param metaHeaders the metadata headers
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType,
                cannedAcl: CannedAcl,
                metaHeaders: MetaHeaders,
                sse: ServerSideEncryption): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, contentType, S3Headers(cannedAcl, metaHeaders), sse)

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, contentType, CannedAcl.Private, MetaHeaders(Map()))

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                contentType: ContentType,
                sse: ServerSideEncryption): Source[ObjectMetadata, NotUsed] =
    putObject(bucket, key, data, contentLength, contentType, CannedAcl.Private, MetaHeaders(Map()), sse)

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long): Source[ObjectMetadata, NotUsed] =
    putObject(bucket,
              key,
              data,
              contentLength,
              ContentTypes.APPLICATION_OCTET_STREAM,
              CannedAcl.Private,
              MetaHeaders(Map()))

  /**
   * Uploads a S3 Object, use this for small files and [[multipartUpload]] for bigger ones
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param data a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]]
   * @param contentLength the number of bytes that will be uploaded (required!)
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]] of the uploaded S3 Object
   */
  def putObject(bucket: String,
                key: String,
                data: Source[ByteString, _],
                contentLength: Long,
                sse: ServerSideEncryption): Source[ObjectMetadata, NotUsed] =
    putObject(bucket,
              key,
              data,
              contentLength,
              ContentTypes.APPLICATION_OCTET_STREAM,
              CannedAcl.Private,
              MetaHeaders(Map()),
              sse)

  private def toJava[M](
      download: akka.stream.scaladsl.Source[Option[
        (akka.stream.scaladsl.Source[ByteString, M], scaladsl.ObjectMetadata)
      ], NotUsed]
  ): Source[Optional[JPair[Source[ByteString, M], ObjectMetadata]], NotUsed] =
    download.map {
      _.map { case (stream, meta) => JPair(stream.asJava, metaDataToJava(meta)) }.asJava
    }.asJava

  /**
   * Downloads a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return A [[akka.japi.Pair]] with a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]], and a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]]
   */
  def download(bucket: String,
               key: String): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    toJava(S3Stream.download(S3Location(bucket, key), None, None, None))

  /**
   * Downloads a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return A [[akka.japi.Pair]] with a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]], and a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]]
   */
  def download(
      bucket: String,
      key: String,
      sse: ServerSideEncryption
  ): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] =
    toJava(S3Stream.download(S3Location(bucket, key), None, None, Some(sse)))

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[akka.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @return A [[akka.japi.Pair]] with a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]], and a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]]
   */
  def download(bucket: String,
               key: String,
               range: ByteRange): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(S3Stream.download(S3Location(bucket, key), Some(scalaRange), None, None))
  }

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[akka.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param sse the server side encryption to use
   * @return A [[akka.japi.Pair]] with a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]], and a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]]
   */
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      sse: ServerSideEncryption
  ): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(S3Stream.download(S3Location(bucket, key), Some(scalaRange), None, Some(sse)))
  }

  /**
   * Downloads a specific byte range of a S3 Object
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param range the [[akka.http.javadsl.model.headers.ByteRange ByteRange]] you want to download
   * @param versionId optional version id of the object
   * @param sse the server side encryption to use
   * @return A [[akka.japi.Pair]] with a [[akka.stream.javadsl.Source Source]] of [[akka.util.ByteString ByteString]], and a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[ObjectMetadata]]
   */
  def download(
      bucket: String,
      key: String,
      range: ByteRange,
      versionId: Optional[String],
      sse: ServerSideEncryption
  ): Source[Optional[JPair[Source[ByteString, NotUsed], ObjectMetadata]], NotUsed] = {
    val scalaRange = range.asInstanceOf[ScalaByteRange]
    toJava(
      S3Stream.download(S3Location(bucket, key), Option(scalaRange), Option(versionId.orElse(null)), Option(sse))
    )
  }

  /**
   * Will return a source of object metadata for a given bucket with optional prefix using version 2 of the List Bucket API.
   * This will automatically page through all keys with the given parameters.
   *
   * The <code>akka.stream.alpakka.s3.list-bucket-api-version</code> can be set to 1 to use the older API version 1
   *
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html  (version 1 API)
   * @see https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html (version 1 API)
   *
   * @param bucket Which bucket that you list object metadata for
   * @param prefix Prefix of the keys you want to list under passed bucket
   * @return Source of object metadata
   */
  def listBucket(bucket: String, prefix: Option[String]): Source[ListBucketResultContents, NotUsed] =
    S3Stream
      .listBucket(bucket, prefix)
      .map { scalaContents =>
        listingToJava(scalaContents)
      }
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      s3Headers: S3Headers): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    S3Stream
      .multipartUpload(S3Location(bucket, key),
                       contentType.asInstanceOf[ScalaContentType],
                       s3Headers,
                       MultipartUploadResult.create)
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @param sse the server side encryption to use
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      s3Headers: S3Headers,
                      sse: ServerSideEncryption): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    S3Stream
      .multipartUpload(S3Location(bucket, key),
                       contentType.asInstanceOf[ScalaContentType],
                       s3Headers,
                       MultipartUploadResult.create,
                       Some(sse))
      .mapMaterializedValue(_.asJava)
      .asJava

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param metaHeaders any meta-headers you want to add
   * @param cannedAcl a [[CannedAcl]], defaults to [[CannedAcl.Private]]
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      cannedAcl: CannedAcl,
                      metaHeaders: MetaHeaders): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, S3Headers(cannedAcl, metaHeaders))

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param metaHeaders any meta-headers you want to add
   * @param cannedAcl a [[CannedAcl]], defaults to [[CannedAcl.Private]]
   * @param sse sse the server side encryption to use
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      cannedAcl: CannedAcl,
                      metaHeaders: MetaHeaders,
                      sse: ServerSideEncryption): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, S3Headers(cannedAcl, metaHeaders), sse)

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param cannedAcl a [[CannedAcl]], defaults to [[CannedAcl.Private]]
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      cannedAcl: CannedAcl): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, cannedAcl, MetaHeaders(Map()))

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param cannedAcl a [[CannedAcl]], defaults to [[CannedAcl.Private]]
   * @param sse the server side encryption to use
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      cannedAcl: CannedAcl,
                      sse: ServerSideEncryption): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, cannedAcl, MetaHeaders(Map()), sse)

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, CannedAcl.Private, MetaHeaders(Map()))

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param sse the server side encryption to use
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      contentType: ContentType,
                      sse: ServerSideEncryption): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, contentType, CannedAcl.Private, MetaHeaders(Map()), sse)

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String, key: String): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, ContentTypes.APPLICATION_OCTET_STREAM, CannedAcl.Private, MetaHeaders(Map()))

  /**
   * Uploads a S3 Object by making multiple requests
   *
   * @param bucket the s3 bucket name
   * @param key the s3 object key
   * @param sse the server side encryption to use
   * @return a [[akka.stream.javadsl.Sink Sink]] that accepts [[akka.util.ByteString ByteString]]'s and materializes to a [[java.util.concurrent.CompletionStage CompletionStage]] of [[MultipartUploadResult]]
   */
  def multipartUpload(bucket: String,
                      key: String,
                      sse: ServerSideEncryption): Sink[ByteString, Source[MultipartUploadResult, NotUsed]] =
    multipartUpload(bucket, key, ContentTypes.APPLICATION_OCTET_STREAM, CannedAcl.Private, MetaHeaders(Map()), sse)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param sourceVersionId version id of source object, if the versioning is enabled in source bucket
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[akka.stream.alpakka.s3.javadsl.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
                    sourceKey: String,
                    targetBucket: String,
                    targetKey: String,
                    sourceVersionId: Optional[String],
                    contentType: ContentType,
                    s3Headers: S3Headers,
                    sse: ServerSideEncryption): RunnableGraph[Source[MultipartUploadResult, NotUsed]] =
    RunnableGraph
      .fromGraph {
        S3Stream
          .multipartCopy(
            S3Location(sourceBucket, sourceKey),
            S3Location(targetBucket, targetKey),
            Option(sourceVersionId.orElse(null)),
            contentType.asInstanceOf[ScalaContentType],
            s3Headers,
            MultipartUploadResult.create,
            Option(sse)
          )
      }
      .mapMaterializedValue(_.asJava)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param sourceVersionId version id of source object, if the versioning is enabled in source bucket
   * @param s3Headers any headers you want to add
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[akka.stream.alpakka.s3.javadsl.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
                    sourceKey: String,
                    targetBucket: String,
                    targetKey: String,
                    sourceVersionId: Optional[String],
                    s3Headers: S3Headers,
                    sse: ServerSideEncryption): RunnableGraph[Source[MultipartUploadResult, NotUsed]] =
    multipartCopy(sourceBucket,
                  sourceKey,
                  targetBucket,
                  targetKey,
                  sourceVersionId,
                  ContentTypes.APPLICATION_OCTET_STREAM,
                  s3Headers,
                  sse)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param contentType an optional [[akka.http.javadsl.model.ContentType ContentType]]
   * @param s3Headers any headers you want to add
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[akka.stream.alpakka.s3.javadsl.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
                    sourceKey: String,
                    targetBucket: String,
                    targetKey: String,
                    contentType: ContentType,
                    s3Headers: S3Headers,
                    sse: ServerSideEncryption): RunnableGraph[Source[MultipartUploadResult, NotUsed]] =
    multipartCopy(sourceBucket, sourceKey, targetBucket, targetKey, Optional.empty(), contentType, s3Headers, sse)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @param s3Headers any headers you want to add
   * @param sse the server side encryption to use
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[akka.stream.alpakka.s3.javadsl.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
                    sourceKey: String,
                    targetBucket: String,
                    targetKey: String,
                    s3Headers: S3Headers,
                    sse: ServerSideEncryption): RunnableGraph[Source[MultipartUploadResult, NotUsed]] =
    multipartCopy(sourceBucket,
                  sourceKey,
                  targetBucket,
                  targetKey,
                  ContentTypes.APPLICATION_OCTET_STREAM,
                  s3Headers,
                  sse)

  /**
   * Copy a S3 Object by making multiple requests.
   *
   * @param sourceBucket the source s3 bucket name
   * @param sourceKey the source s3 key
   * @param targetBucket the target s3 bucket name
   * @param targetKey the target s3 key
   * @return a [[java.util.concurrent.CompletionStage CompletionStage]] containing the [[akka.stream.alpakka.s3.javadsl.MultipartUploadResult MultipartUploadResult]] of the uploaded S3 Object
   */
  def multipartCopy(sourceBucket: String,
                    sourceKey: String,
                    targetBucket: String,
                    targetKey: String): RunnableGraph[Source[MultipartUploadResult, NotUsed]] =
    multipartCopy(sourceBucket,
                  sourceKey,
                  targetBucket,
                  targetKey,
                  ContentTypes.APPLICATION_OCTET_STREAM,
                  S3Headers.empty,
                  null)

  private def listingToJava(scalaContents: scaladsl.ListBucketResultContents): ListBucketResultContents =
    ListBucketResultContents(scalaContents.bucketName,
                             scalaContents.key,
                             scalaContents.eTag,
                             scalaContents.size,
                             scalaContents.lastModified,
                             scalaContents.storageClass)

  private def metaDataToJava(scalaContents: scaladsl.ObjectMetadata): ObjectMetadata =
    new ObjectMetadata(scalaContents)
}
