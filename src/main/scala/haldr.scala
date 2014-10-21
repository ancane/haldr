package object haldr extends HalJsonProtocol {

  import spray.http.Uri, Uri._
  import uritemplate.URITemplate

  implicit class UriInterpolators(val sc: StringContext) extends AnyVal {
    def u(args: Any*): LinkUri      = LinkUri(Path(sc.s(args: _*)))
    def r(args: Any*): RelativeUri = RelativeUri(Path(sc.s(args: _*)))
    def t(args: Any*): UriTpl       = {
      val encArgs = args.map(x => (Path / x.toString).toString.tail)
      val src = sc.s(encArgs: _*)
      URITemplate.parse(src).fold(sys.error, _ => UriTpl(src))
    }
  }

  implicit class StringProps(path: String) {
    def prop(key: String, value: String): LinkObject =
      LinkUri(Path(path)).prop(key, value)

    def enc: String = (Path / path).toString.tail
  }

  implicit class PathProps(path: Path) {
    def prop(key: String, value: String): LinkObject =
      LinkUri(path).prop(key, value)
  }

}
