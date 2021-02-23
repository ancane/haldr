package haldr

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol._
import spray.json._
import akka.stream.ActorMaterializer

class HaldrRouteExampleSpec extends Specification with Specs2RouteTest {

  implicit val actorMaterializer = ActorMaterializer()

  case class Car(make: String, model: String)
  implicit val carF = jsonFormat2(Car)

  val route =
    get {
      path("car") {
        complete(
          Resource(Car("BMW", "i3"), u"/cars/bmw/i3")
        )
      }
    }

  val testCar = """
    {
      "make": "BMW",
      "model": "i3",
      "_links": {
         "self": {"href": "/cars/bmw/i3"}
      }
    }""".parseJson


  "route" should {
    "get car" in {
      Get("/car") ~> Route.seal(route) ~> check {
        responseAs[JsObject] === testCar
      }
    }
  }


}
