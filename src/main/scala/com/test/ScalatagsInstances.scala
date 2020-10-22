package com.test

import org.http4s.{Charset, DefaultCharset, EntityEncoder, MediaType}
import org.http4s.MediaType.text.html
import org.http4s.headers.`Content-Type`
import scalatags.Text.TypedTag

trait ScalatagsInstances {
  implicit def htmlContentEncoder[F[_]](implicit charset: Charset = DefaultCharset): EntityEncoder[F, TypedTag[String]] =
    contentEncoder(html)

  private def contentEncoder[F[_], C <: TypedTag[String]](mediaType: MediaType)(implicit charset: Charset = DefaultCharset): EntityEncoder[F, C] =
    EntityEncoder.stringEncoder(charset).contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))
}

object ScalatagsInstances extends ScalatagsInstances
