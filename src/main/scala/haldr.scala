import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path

package object haldr extends HalJsonProtocol {

  implicit class UriInterpolators(val sc: StringContext) extends AnyVal {
    def u(args: Any*): LinkUri     = LinkUri(Uri.Empty.withPath(Path(sc.s(args: _*))))
    def r(args: Any*): RelativeUri = RelativeUri(Uri.Empty.withPath(Path(sc.s(args: _*))))
    def t(args: Any*): UriTpl      = {
      val encArgs = args.map(x => (Path / x.toString).toString.tail)
      val src = sc.s(encArgs: _*)
      UriTpl(src)
    }
  }

  implicit class StringProps(path: String) {
    def prop(key: String, value: String): LinkObject =
      LinkUri(Uri.Empty.withPath(Path(path))).prop(key, value)

    def enc: String = (Path / path).toString.tail
  }

  implicit class PathProps(path: Path) {
    def prop(key: String, value: String): LinkObject =
      LinkUri(Uri.Empty.withPath(path)).prop(key, value)
  }

}
