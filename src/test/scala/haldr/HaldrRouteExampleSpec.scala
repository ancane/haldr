package haldr

import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables
import org.specs2.specification.Fragments
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest

import spray.routing.HttpService
import spray.http.StatusCodes._

import spray.json._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import haldr._

class HaldrRouteExampleSpec extends Specification with Specs2RouteTest with HttpService {
  def actorRefFactory = system

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
      Get("/car") ~> sealRoute(route) ~> check {
        responseAs[JsObject] === testCar
      }
    }
  }


}
