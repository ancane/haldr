package haldr

import spray.json._
import spray.http.Uri, Uri._
import scala.collection.immutable.ListMap
import DefaultJsonProtocol._

private[haldr] object HalJsonProtocol extends HalJsonProtocol

private[haldr] trait HalJsonProtocol {

  implicit def resourceFormat[T]:RootJsonFormat[Resource] = rootFormat(lazyFormat(lift(new JsonWriter[Resource] {

    def formatLink(path: String, props: List[(String, JsValue)]): JsObject =
      JsObject(ListMap("href" -> JsString(path)) ++ props)

    def formatLink(uri: Uri, props: List[(String, JsValue)]): JsObject =
      formatLink(uri.toString, props)

    def write(r: Resource): JsValue = {
      val embeds: ListMap[String, JsValue] = if (r.embeds.isEmpty) ListMap.empty
      else ListMap("_embedded" -> JsObject(
        r.embeds.result.foldLeft(ListMap[String, JsValue]()){ case (acc, emb) => emb match {
          case (rel, Right(obj))      => acc + (rel -> obj)
          case (rel, Left(Left(res))) => acc + (rel ->
              ((r.baseUri, res.baseUri) match {
                case (Some(LinkUri(u)), Some(RelativeUri(p))) =>
                  new Resource(res.resource, Some(LinkUri(p.withPath(u.path ++ p.path))), res.links, res.embeds)
                case _ => res
              }).toJson)
          case (rel, Left(Right(ress))) => acc + (rel -> ress.map{res=>
            ((r.baseUri, res.baseUri) match {
              case (Some(LinkUri(u)), Some(RelativeUri(p))) =>
                new Resource(res.resource, Some(LinkUri(p.withPath(u.path ++ p.path))), res.links, res.embeds)
              case _ => res
            })}.toJson)
        }}))

      val self: ListMap[String, JsObject] = r.baseUri match {
        case Some(x@LinkUri(_)) => ListMap("self" -> formatLink(x.uri, x.props))
        case Some(x@RelativeUri(_)) => ListMap("self" -> formatLink(x.uri, x.props))
        case Some(x@UriString(_)) => ListMap("self" -> formatLink(x.str, x.props))
        case Some(UriTpl(_)) =>
          sys.error("Are you sure about templated self link? It's much better to use absolute URI instead")
        case _ => ListMap.empty
      }

      val links: List[(String, JsValue)] = if (self.isEmpty && r.links.isEmpty) Nil
      else {
        def linksArrayToJsValue(rel: String, links: List[LinkObject]): JsValue = {
          JsArray(links.map(singleLinkToJsValue): _*)
        }

        def singleLinkToJsValue(link: LinkObject): JsValue = link match {
          case x: LinkUri => formatLink(x.uri, x.props)
          case x: UriTpl => formatLink(x.tpl, x.props)
          case x: UriString => formatLink(x.str, x.props)
          case x: RelativeUri => r.baseUri match {
            case Some(y: LinkUri) => formatLink(x.uri.withPath(y.uri.path ++ x.uri.path), x.props)
            case _ => formatLink(x.uri, x.props)
          }
        }

        List("_links" -> JsObject(
          self ++ r.links.result.groupBy(_._1).foldLeft(List[(String, JsValue)]()) {
            case (acc, (rel, List(lnk))) => rel -> singleLinkToJsValue(lnk._2) :: acc
            case (acc, (rel, lnks)) => rel -> linksArrayToJsValue(rel, lnks.map(_._2)) :: acc
          }))
      }

      JsObject(ListMap.empty ++ r.resource.fields ++ embeds ++ links)
    }
  })))
}
