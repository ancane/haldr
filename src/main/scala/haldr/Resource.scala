package haldr

import spray.json._
import spray.http.Uri, Uri._
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer
import DefaultJsonProtocol._
import HalJsonProtocol._

class Resource private[haldr] (
  private[haldr] val resource : JsObject,
  private[haldr] val baseUri  : Option[LinkObject] = None,
  private[haldr] val links    : ListBuffer[(String, LinkObject)] = ListBuffer.empty,
  private[haldr] val embeds   : ListBuffer[(String, Either[Either[Resource, Seq[Resource]], JsValue])] = ListBuffer.empty
) {

  def link[T <: LinkObject](rel: String, lnk: T): Resource = {
    lnk match {
      case x: UriTpl => links += (rel -> x.prop("templated", JsTrue))
      case x =>         links += (rel -> x)
    }
    this
  }

  def link(rel: String, path: String): Resource = {
    links += (rel -> UriString(path))
    this
  }

  def link(rel: String, path: Path): Resource = {
    links += (rel -> LinkUri(Uri.Empty.withPath(path)))
    this
  }

  def link[T <: LinkObject](x :(String, T)): Resource = {
    val (rel, lnk) = x
    link(rel, lnk)
  }

  def embed(rel: String, x: Resource): Resource = {
    embeds += (rel -> Left(Left(x)))
    this
  }

  def embed(rel: String, x: Seq[Resource]): Resource = {
    embeds += (rel -> Left(Right(x)))
    this
  }

  def embed[T: JsonWriter](rel: String, x: T): Resource = {
    embeds += (rel -> Right(x.toJson))
    this
  }

  def embed(x :(String, Resource)): Resource = {
    val (rel, res) = x
    embed(rel, res)
  }
}

object Resource {
  def apply() = new Resource(JsObject(), None)
  def apply(uri: LinkObject) = new Resource(JsObject(), Some(uri))
  def apply[T: JsonWriter](x: T) = new Resource(x.toJson.asJsObject, None)
  def apply[T: JsonWriter](x: T, uri: LinkObject) = new Resource(x.toJson.asJsObject, Some(uri))
}

private[haldr] sealed trait LinkObject {
  private [this] val _props = ListBuffer[(String, JsValue)]()

  def prop(p: String, value: String): LinkObject = {
    prop(p, JsString(value))
  }

  def prop(p: String, value: JsValue): LinkObject = {
    _props += (p -> value)
    this
  }

  def props = _props.result
}

private[haldr] case class LinkUri(uri:Uri) extends LinkObject {
  def / (x: String)      = LinkUri(uri.withPath(path = uri.path / x))
  def / (x: Path)        = LinkUri(uri.withPath(path = uri.path ++ x))
  def / (x: RelativeUri) = LinkUri(uri.withPath(path = uri.path ++ x.uri.path))
  def q (x: String)      = LinkUri(uri.withQuery(x))
  def q (x: (String, String)) = LinkUri(uri.withQuery(x))
}

private[haldr] case class RelativeUri(uri:Uri) extends LinkObject {
  def / (x: String)      = RelativeUri(uri.withPath(path = uri.path / x))
  def / (x: Path)        = RelativeUri(uri.withPath(path = uri.path ++ x))
  def / (x: RelativeUri) = RelativeUri(uri.withPath(path = uri.path ++ x.uri.path))
  def q (x: String)      = RelativeUri(uri.withQuery(x))
  def q (x: (String, String)) = RelativeUri(uri.withQuery(x))
}

private[haldr] case class UriTpl(tpl:String) extends LinkObject

private[haldr] case class UriString(str:String) extends LinkObject
